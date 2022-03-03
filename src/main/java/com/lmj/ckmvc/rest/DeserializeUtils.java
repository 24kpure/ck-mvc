package com.lmj.ckmvc.rest;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

import java.io.IOException;

/**
 * @Author: lmj
 * @Description:
 * @Date: Create in 11:55 上午 2021/4/12
 **/
public class DeserializeUtils {
    public final static ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.addHandler(new DeserializationProblemHandler() {
            @Override
            public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert,
                                                 String failureMsg) throws IOException {
                if (targetType == Boolean.class) {
                    return "1".equalsIgnoreCase(valueToConvert);
                }
                return super.handleWeirdStringValue(ctxt, targetType, valueToConvert, failureMsg);
            }
        });
    }

    public static void main(String[] args) {

    }
}