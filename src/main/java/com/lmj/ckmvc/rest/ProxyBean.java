package com.lmj.ckmvc.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.lmj.ckmvc.constant.CanalFieldEnum;
import com.lmj.ckmvc.constant.CanalTypeEnum;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: lmj
 * @Description:
 * @Date: Create in 4:11 下午 2021/3/26
 **/
public class ProxyBean implements MethodInterceptor {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    private final static Map<Method, Set<CanalTypeEnum>> HANDLE_TYPE = new ConcurrentHashMap<>(16);

    private final static Map<Method, Set<String>> TABLE_NAME_MAP = new ConcurrentHashMap<>(16);

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final Object[] args = invocation.getArguments();
        final Method method = invocation.getMethod();

        final Set<CanalTypeEnum> canalTypeEnums = HANDLE_TYPE.get(method);
        if (CollectionUtils.isEmpty(canalTypeEnums)) {
            return invocation.proceed();
        }

        //get msgContent
        String rawMsg = null;
        for (Object arg : args) {
            if (arg instanceof ConsumerRecord) {
                rawMsg = ((ConsumerRecord<String, String>) arg).value();
                break;
            } else if (arg instanceof List) {
                rawMsg = ((List<String>) arg).get(0);
                break;
            }
        }
        Assert.notNull(rawMsg, "unSupport assignment for:" + args[0].getClass());

        if (StringUtils.isEmpty(rawMsg)) {
            paramAck(args);
            return null;
        }

        // handleType filter
        JsonNode rowData = MAPPER.readTree(rawMsg);
        String handleType = rowData.get(CanalFieldEnum.TYPE.getKey()).asText();
        if (!canalTypeEnums.contains(CanalTypeEnum.fromType(handleType))) {
            paramAck(args);
            return null;
        }

        //table filter
        final Set<String> tableSet = TABLE_NAME_MAP.get(method);
        String tableName = rowData.get(CanalFieldEnum.TABLE.getKey()).asText();
        if (CollectionUtils.isEmpty(tableSet) || !tableSet.contains(tableName)) {
            paramAck(args);
            return null;
        }

        // param parse
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof CanalTypeEnum) {
                args[i] = CanalTypeEnum.fromType(handleType);
            }
            if (args[i] instanceof List) {
                final String className = ((ParameterizedTypeImpl) method.getGenericParameterTypes()[0])
                        .getActualTypeArguments()[0]
                        .getTypeName();

                args[i] = MAPPER.convertValue(rowData.get(CanalFieldEnum.DATA.getKey()),
                        MAPPER.getTypeFactory().constructCollectionType(List.class, Class.forName(className)));
            }
        }

        return invocation.proceed();
    }

    private void paramAck(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Acknowledgment) {
                ((Acknowledgment) arg).acknowledge();
                return;
            }
        }
    }

    public static void register(Method method, Set<CanalTypeEnum> canalTypeEnumSet, Set<String> tableSet) {
        HANDLE_TYPE.put(method, canalTypeEnumSet);
        TABLE_NAME_MAP.put(method, tableSet);
    }
}