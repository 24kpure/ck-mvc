package com.lmj.ckmvc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: lmj
 * @Description:
 * @Date: Create in 3:55 下午 2021/3/24
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface CanalController {

    String[] topics() default "";

    String consumerGroup() default "";

    String concurrency() default "";

    String[] properties() default {};
}
