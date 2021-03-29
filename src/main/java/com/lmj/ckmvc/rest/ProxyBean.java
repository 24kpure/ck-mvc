package com.lmj.ckmvc.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.lmj.ckmvc.constant.CanalFieldEnum;
import com.lmj.ckmvc.constant.CanalTypeEnum;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
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
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        final Set<CanalTypeEnum> canalTypeEnums = HANDLE_TYPE.get(method);
        if (CollectionUtils.isEmpty(canalTypeEnums)) {
            return methodProxy.invokeSuper(o, args);
        }

        String rawMsg = null;
        if (args[0] instanceof ConsumerRecord) {
            rawMsg = ((ConsumerRecord<String, String>) args[0]).value();
        } else if (args[0] instanceof List) {
            rawMsg = ((List<String>) args[0]).get(0);
        }

        Assert.notNull(rawMsg, "unSupport assignment for:" + args[0].getClass());


        if (StringUtils.isEmpty(rawMsg)) {
            paramAck(args);
            return null;
        }

        JsonNode rowData = MAPPER.readTree(rawMsg);

        // handleType filter
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

        if (args[0] instanceof List) {
            final String className = ((ParameterizedTypeImpl) method.getGenericParameterTypes()[0])
                    .getActualTypeArguments()[0]
                    .getTypeName();

            args[0] = MAPPER.convertValue(rowData.get(CanalFieldEnum.DATA.getKey()),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Class.forName(className)));
        }

        return methodProxy.invokeSuper(o, args);
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