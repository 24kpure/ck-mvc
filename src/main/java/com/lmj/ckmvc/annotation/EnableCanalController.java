package com.lmj.ckmvc.annotation;

import com.lmj.ckmvc.rest.CanalControllerBootstrapConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: lmj
 * @Description:
 * @Date: Create in 2:27 下午 2021/3/25
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CanalControllerBootstrapConfiguration.class)
public @interface EnableCanalController {
}
