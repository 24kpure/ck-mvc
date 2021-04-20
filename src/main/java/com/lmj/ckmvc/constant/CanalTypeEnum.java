package com.lmj.ckmvc.constant;

import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public enum CanalTypeEnum {

    /**
     * 插入操作
     */
    INSERT("INSERT", "插入操作", RequestMethod.POST),

    /**
     * 更新操作
     */
    UPDATE("UPDATE", "更新操作", RequestMethod.PUT),

    /**
     * 删除操作
     */
    DELETE("DELETE", "删除操作", RequestMethod.DELETE),

    /**
     * 查询操作
     */
    SELECT("SELECT", "查询操作", RequestMethod.GET)
    ;

    private String key;

    private String value;

    private RequestMethod requestMethod;

    CanalTypeEnum(String key, String value, RequestMethod requestMethod) {
        this.key = key;
        this.value = value;
        this.requestMethod = requestMethod;
    }

    public String getKey() {
        return key;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    private static Map<String, CanalTypeEnum> MAP = Arrays.stream(values())
            .collect(Collectors.collectingAndThen(Collectors.toMap(CanalTypeEnum::getKey, Function.identity()),
                    Collections::unmodifiableMap));

    private static Map<RequestMethod, CanalTypeEnum> METHOD_MAP = Arrays.stream(values())
            .collect(Collectors.collectingAndThen(Collectors.toMap(CanalTypeEnum::getRequestMethod, Function.identity()),
                    Collections::unmodifiableMap));

    public static CanalTypeEnum fromType(String type) {
        return MAP.get(type);
    }

    public static CanalTypeEnum fromType(RequestMethod method) {
        return METHOD_MAP.get(method);
    }
}
