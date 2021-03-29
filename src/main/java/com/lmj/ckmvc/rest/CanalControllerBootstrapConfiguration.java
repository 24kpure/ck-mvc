/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lmj.ckmvc.rest;

import com.lmj.ckmvc.annotation.EnableCanalController;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

/**
 * An {@link ImportBeanDefinitionRegistrar} class that registers a {@link KafkaListenerAnnotationBeanPostProcessor}
 * bean capable of processing Spring's @{@link KafkaListener} annotation. Also register
 * a default {@link KafkaListenerEndpointRegistry}.
 *
 * <p>This configuration class is automatically imported when using the @{@link EnableKafka}
 * annotation.  See {@link EnableKafka} Javadoc for complete usage.
 *
 * @author Stephane Nicoll
 * @author Gary Russell
 * @author Artem Bilan
 * @see KafkaListenerAnnotationBeanPostProcessor
 * @see KafkaListenerEndpointRegistry
 * @see EnableCanalController
 */
public class CanalControllerBootstrapConfiguration implements ImportBeanDefinitionRegistrar {

    /**
     * The bean name of the internally managed Kafka listener annotation processor.
     */
    private static final String CANAL_CONTROL_ANNOTATION_PROCESSOR_BEAN_NAME =
            "com.pupu.consumer.coupon.rest.CanalControllerAnnotationBeanPostProcessor";


    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(
                CANAL_CONTROL_ANNOTATION_PROCESSOR_BEAN_NAME)) {

            registry.registerBeanDefinition(CANAL_CONTROL_ANNOTATION_PROCESSOR_BEAN_NAME,
                    new RootBeanDefinition(CanalControllerAnnotationBeanPostProcessor.class));
        }
    }
}
