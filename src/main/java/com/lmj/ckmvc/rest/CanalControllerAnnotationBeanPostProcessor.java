/*
 * Copyright 2014-2019 the original author or authors.
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

import com.lmj.ckmvc.annotation.CanalController;
import com.lmj.ckmvc.constant.CanalTypeEnum;
import lombok.Data;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.log.LogAccessor;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.KafkaListenerConfigUtils;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.kafka.config.MultiMethodKafkaListenerEndpoint;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Bean post-processor that registers methods annotated with {@link KafkaListener}
 * to be invoked by a Kafka message listener container created under the covers
 * by a {@link KafkaListenerContainerFactory}
 * according to the parameters of the annotation.
 *
 * <p>Annotated methods can use flexible arguments as defined by {@link KafkaListener}.
 *
 * <p>This post-processor is automatically registered by Spring's {@link EnableKafka}
 * annotation.
 *
 * <p>Auto-detect any {@link KafkaListenerConfigurer} instances in the container,
 * allowing for customization of the registry to be used, the default container
 * factory or for fine-grained control over endpoints registration. See
 * {@link EnableKafka} Javadoc for complete usage details.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Gary Russell
 * @author Artem Bilan
 * @author Dariusz Szablinski
 * @author Venil Noronha
 * @author Dimitri Penner
 * @see KafkaListener
 * @see KafkaListenerErrorHandler
 * @see EnableKafka
 * @see KafkaListenerConfigurer
 * @see KafkaListenerEndpointRegistrar
 * @see KafkaListenerEndpointRegistry
 * @see org.springframework.kafka.config.KafkaListenerEndpoint
 * @see MethodKafkaListenerEndpoint
 */
public class CanalControllerAnnotationBeanPostProcessor<K, V>
        implements BeanPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {
    /**
     * The bean name of the default {@link KafkaListenerContainerFactory}.
     */
    public static final String DEFAULT_KAFKA_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "kafkaListenerContainerFactory";

    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

    private final LogAccessor logger = new LogAccessor(LogFactory.getLog(getClass()));

    private final ListenerScope listenerScope = new ListenerScope();

    private KafkaListenerEndpointRegistry endpointRegistry;

    private String defaultContainerFactoryBeanName = DEFAULT_KAFKA_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

    private BeanFactory beanFactory;

    private final KafkaHandlerMethodFactoryAdapter messageHandlerMethodFactory =
            new KafkaHandlerMethodFactoryAdapter();

    private final KafkaListenerEndpointRegistrar registrar = new KafkaListenerEndpointRegistrar();

    private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    private BeanExpressionContext expressionContext;

    private Charset charset = StandardCharsets.UTF_8;

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    /**
     * Set the {@link KafkaListenerEndpointRegistry} that will hold the created
     * endpoint and manage the lifecycle of the related listener container.
     *
     * @param endpointRegistry the {@link KafkaListenerEndpointRegistry} to set.
     */
    public void setEndpointRegistry(KafkaListenerEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    /**
     * Set the name of the {@link KafkaListenerContainerFactory} to use by default.
     * <p>If none is specified, "kafkaListenerContainerFactory" is assumed to be defined.
     *
     * @param containerFactoryBeanName the {@link KafkaListenerContainerFactory} bean name.
     */
    public void setDefaultContainerFactoryBeanName(String containerFactoryBeanName) {
        this.defaultContainerFactoryBeanName = containerFactoryBeanName;
    }

    /**
     * Set the {@link MessageHandlerMethodFactory} to use to configure the message
     * listener responsible to serve an endpoint detected by this processor.
     * <p>By default, {@link DefaultMessageHandlerMethodFactory} is used and it
     * can be configured further to support additional method arguments
     * or to customize conversion and validation support. See
     * {@link DefaultMessageHandlerMethodFactory} Javadoc for more details.
     *
     * @param messageHandlerMethodFactory the {@link MessageHandlerMethodFactory} instance.
     */
    public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
        this.messageHandlerMethodFactory.setHandlerMethodFactory(messageHandlerMethodFactory);
    }

    /**
     * Making a {@link BeanFactory} available is optional; if not set,
     * {@link KafkaListenerConfigurer} beans won't get autodetected and an
     * {@link #setEndpointRegistry endpoint registry} has to be explicitly configured.
     *
     * @param beanFactory the {@link BeanFactory} to be used.
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            this.resolver = ((ConfigurableListableBeanFactory) beanFactory).getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext((ConfigurableListableBeanFactory) beanFactory,
                    this.listenerScope);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.registrar.setBeanFactory(this.beanFactory);

        if (this.beanFactory instanceof ListableBeanFactory) {
            Map<String, KafkaListenerConfigurer> instances =
                    ((ListableBeanFactory) this.beanFactory).getBeansOfType(KafkaListenerConfigurer.class);
            for (KafkaListenerConfigurer configurer : instances.values()) {
                configurer.configureKafkaListeners(this.registrar);
            }
        }

        if (this.registrar.getEndpointRegistry() == null) {
            if (this.endpointRegistry == null) {
                Assert.state(this.beanFactory != null,
                        "BeanFactory must be set to find endpoint registry by bean name");
                this.endpointRegistry = this.beanFactory.getBean(
                        KafkaListenerConfigUtils.KAFKA_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME,
                        KafkaListenerEndpointRegistry.class);
            }
            this.registrar.setEndpointRegistry(this.endpointRegistry);
        }

        if (this.defaultContainerFactoryBeanName != null) {
            this.registrar.setContainerFactoryBeanName(this.defaultContainerFactoryBeanName);
        }

        // Set the custom handler method factory once resolved by the configurer
        MessageHandlerMethodFactory handlerMethodFactory = this.registrar.getMessageHandlerMethodFactory();
        if (handlerMethodFactory != null) {
            this.messageHandlerMethodFactory.setHandlerMethodFactory(handlerMethodFactory);
        } else {
            addFormatters(this.messageHandlerMethodFactory.defaultFormattingConversionService);
        }

        // Actually register all listeners
        this.registrar.afterPropertiesSet();
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        if (!this.nonAnnotatedClasses.contains(bean.getClass())) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            Collection<CanalController> classLevelListeners = findListenerAnnotations(targetClass);

            final List<Method> multiMethods = new ArrayList<>();

            Assert.isTrue(classLevelListeners.size() <= 1, "暂不支持多个CanalController");
            if (classLevelListeners.size() <= 0) {
                this.nonAnnotatedClasses.add(bean.getClass());
                return bean;
            }

            Set<Method> methodsWithHandler = MethodIntrospector.selectMethods(targetClass,
                    (ReflectionUtils.MethodFilter) method ->
                            AnnotationUtils.findAnnotation(method, RequestMapping.class) != null);
            multiMethods.addAll(methodsWithHandler);

            processMultiMethodListeners(classLevelListeners.stream().findFirst().orElse(null), multiMethods, bean, beanName);
        }
        return bean;
    }

    /*
     * AnnotationUtils.getRepeatableAnnotations does not look at interfaces
     */
    private Collection<CanalController> findListenerAnnotations(Class<?> clazz) {
        Set<CanalController> listeners = new HashSet<>();
        CanalController ann = AnnotationUtils.findAnnotation(clazz, CanalController.class);
        if (ann != null) {
            listeners.add(ann);
        }

        return listeners;
    }

    @Data
    private class MappingConfig {
        private Set<String> pathSet;

        private String name;

        private Set<RequestMethod> requestMethodSet;
    }

    private MappingConfig findRequestMapping(Method method) {
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        if (requestMapping == null) {
            return null;
        }
        MappingConfig config = new MappingConfig();

        config.setName(requestMapping.name());
        config.setPathSet(Arrays.stream(requestMapping.path()).collect(Collectors.toSet()));
        config.setRequestMethodSet(Arrays.stream(requestMapping.method()).collect(Collectors.toSet()));

        if (requestMapping.value().length > 0) {
            return config;
        }

        //extend annotation can't get value & name
        do {
            PutMapping put = AnnotationUtils.findAnnotation(method, PutMapping.class);
            if (put != null) {
                config.setName(put.name());
                config.getPathSet().addAll(Arrays.stream(put.path()).collect(Collectors.toSet()));
                break;
            }

            PostMapping post = AnnotationUtils.findAnnotation(method, PostMapping.class);
            if (post != null) {
                config.setName(post.name());
                config.getPathSet().addAll(Arrays.stream(post.path()).collect(Collectors.toSet()));
                break;
            }

            DeleteMapping delete = AnnotationUtils.findAnnotation(method, DeleteMapping.class);
            if (delete != null) {
                config.setName(delete.name());
                config.getPathSet().addAll(Arrays.stream(delete.path()).collect(Collectors.toSet()));
                break;
            }
        }
        while (false);

        return config;
    }

    private void processMultiMethodListeners(CanalController classLevelListener, List<Method> multiMethods,
                                             Object bean, String beanName) {
        //proxy factory
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(bean);
        proxyFactory.addAdvice(new ProxyBean());
        Object proxyBean = proxyFactory.getProxy();

        for (Method method : multiMethods) {
            Method checked = checkProxy(method, bean);
            MappingConfig requestMapping = findRequestMapping(method);
            if (requestMapping == null) {
                continue;
            }

            final Set<CanalTypeEnum> handleTypeSet = requestMapping.getRequestMethodSet().stream()
                    .map(CanalTypeEnum::fromType)
                    .collect(Collectors.toSet());

            if (CollectionUtils.isEmpty(handleTypeSet)) {
                handleTypeSet.add(CanalTypeEnum.DELETE);
                handleTypeSet.add(CanalTypeEnum.INSERT);
                handleTypeSet.add(CanalTypeEnum.UPDATE);
            }

            ProxyBean.register(method, handleTypeSet, requestMapping.getPathSet());

            MultiMethodKafkaListenerEndpoint<K, V> endpoint =
                    new MultiMethodKafkaListenerEndpoint<>(Collections.singletonList(checked), checked, proxyBean);
            processListener(endpoint, classLevelListener, proxyBean, bean.getClass(), beanName, classLevelListener.consumerGroup() + requestMapping.getName());
        }
    }

    private Method checkProxy(Method methodArg, Object bean) {
        Method method = methodArg;
        if (AopUtils.isJdkDynamicProxy(bean)) {
            try {
                // Found a @KafkaListener method on the target class for this JDK proxy ->
                // is it also present on the proxy itself?
                method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
                Class<?>[] proxiedInterfaces = ((Advised) bean).getProxiedInterfaces();
                for (Class<?> iface : proxiedInterfaces) {
                    try {
                        method = iface.getMethod(method.getName(), method.getParameterTypes());
                        break;
                    } catch (@SuppressWarnings("unused") NoSuchMethodException noMethod) {
                        // NOSONAR
                    }
                }
            } catch (SecurityException ex) {
                ReflectionUtils.handleReflectionException(ex);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException(String.format(
                        "@KafkaListener method '%s' found on bean target class '%s', " +
                                "but not found in any interface(s) for bean JDK proxy. Either " +
                                "pull the method up to an interface or switch to subclass (CGLIB) " +
                                "proxies by setting proxy-target-class/proxyTargetClass " +
                                "attribute to 'true'", method.getName(),
                        method.getDeclaringClass().getSimpleName()), ex);
            }
        }
        return method;
    }

    protected void processListener(MethodKafkaListenerEndpoint<?, ?> endpoint, CanalController canalController, Object bean,
                                   Object adminTarget, String beanName, String consumerGroup) {

        String beanRef = "__listener";
        if (StringUtils.hasText(beanRef)) {
            this.listenerScope.addListener(beanRef, bean);
        }

        resolveKafkaProperties(endpoint, canalController.properties());

        endpoint.setBean(bean);
        endpoint.setMessageHandlerMethodFactory(this.messageHandlerMethodFactory);
        endpoint.setId(consumerGroup);
        endpoint.setGroupId(consumerGroup);
        endpoint.setTopicPartitions(new TopicPartitionOffset[0]);
        endpoint.setTopics(resolveTopics(canalController));
        endpoint.setTopicPattern(null);
        endpoint.setClientIdPrefix(resolveExpressionAsString("", "clientIdPrefix"));

        String concurrency = canalController.concurrency();
        if (StringUtils.hasText(concurrency)) {
            endpoint.setConcurrency(resolveExpressionAsInteger(concurrency, "concurrency"));
        }

        endpoint.setSplitIterables(true);

        KafkaListenerContainerFactory<?> factory = null;
        String containerFactoryBeanName = resolve("");
        if (StringUtils.hasText(containerFactoryBeanName)) {
            Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
            try {
                factory = this.beanFactory.getBean(containerFactoryBeanName, KafkaListenerContainerFactory.class);
            } catch (NoSuchBeanDefinitionException ex) {
                throw new BeanInitializationException("Could not register Kafka listener endpoint on [" + adminTarget
                        + "] for bean " + beanName + ", no " + KafkaListenerContainerFactory.class.getSimpleName()
                        + " with id '" + containerFactoryBeanName + "' was found in the application context", ex);
            }
        }

        endpoint.setBeanFactory(this.beanFactory);
        String errorHandlerBeanName = resolveExpressionAsString("", "errorHandler");
        if (StringUtils.hasText(errorHandlerBeanName)) {
            endpoint.setErrorHandler(this.beanFactory.getBean(errorHandlerBeanName, KafkaListenerErrorHandler.class));
        }
        this.registrar.registerEndpoint(endpoint, factory);
        if (StringUtils.hasText(beanRef)) {
            this.listenerScope.removeListener(beanRef);
        }
    }

    private void resolveKafkaProperties(MethodKafkaListenerEndpoint<?, ?> endpoint, String[] propertyStrings) {
        if (propertyStrings.length > 0) {
            Properties properties = new Properties();
            for (String property : propertyStrings) {
                String value = resolveExpressionAsString(property, "property");
                if (value != null) {
                    try {
                        properties.load(new StringReader(value));
                    } catch (IOException e) {
                        this.logger.error(e, () -> "Failed to load property " + property + ", continuing...");
                    }
                }
            }
            endpoint.setConsumerProperties(properties);
        }
    }

    private String[] resolveTopics(CanalController kafkaListener) {
        String[] topics = kafkaListener.topics();
        List<String> result = new ArrayList<>();
        if (topics.length > 0) {
            for (String topic1 : topics) {
                Object topic = resolveExpression(topic1);
                resolveAsString(topic, result);
            }
        }
        return result.toArray(new String[0]);
    }


    @SuppressWarnings("unchecked")
    private void resolveAsString(Object resolvedValue, List<String> result) {
        if (resolvedValue instanceof String[]) {
            for (Object object : (String[]) resolvedValue) {
                resolveAsString(object, result);
            }
        } else if (resolvedValue instanceof String) {
            result.add((String) resolvedValue);
        } else if (resolvedValue instanceof Iterable) {
            for (Object object : (Iterable<Object>) resolvedValue) {
                resolveAsString(object, result);
            }
        } else {
            throw new IllegalArgumentException(String.format(
                    "@KafKaListener can't resolve '%s' as a String", resolvedValue));
        }
    }

    private String resolveExpressionAsString(String value, String attribute) {
        Object resolved = resolveExpression(value);
        if (resolved instanceof String) {
            return (String) resolved;
        } else if (resolved != null) {
            throw new IllegalStateException("The [" + attribute + "] must resolve to a String. "
                    + "Resolved to [" + resolved.getClass() + "] for [" + value + "]");
        }
        return null;
    }

    private Integer resolveExpressionAsInteger(String value, String attribute) {
        Object resolved = resolveExpression(value);
        Integer result = null;
        if (resolved instanceof String) {
            result = Integer.parseInt((String) resolved);
        } else if (resolved instanceof Number) {
            result = ((Number) resolved).intValue();
        } else if (resolved != null) {
            throw new IllegalStateException(
                    "The [" + attribute + "] must resolve to an Number or a String that can be parsed as an Integer. "
                            + "Resolved to [" + resolved.getClass() + "] for [" + value + "]");
        }
        return result;
    }

    private Object resolveExpression(String value) {
        return this.resolver.evaluate(resolve(value), this.expressionContext);
    }

    /**
     * Resolve the specified value if possible.
     *
     * @param value the value to resolve
     * @return the resolved value
     * @see ConfigurableBeanFactory#resolveEmbeddedValue
     */
    private String resolve(String value) {
        if (this.beanFactory != null && this.beanFactory instanceof ConfigurableBeanFactory) {
            return ((ConfigurableBeanFactory) this.beanFactory).resolveEmbeddedValue(value);
        }
        return value;
    }

    private void addFormatters(FormatterRegistry registry) {
        for (Converter<?, ?> converter : getBeansOfType(Converter.class)) {
            registry.addConverter(converter);
        }
        for (GenericConverter converter : getBeansOfType(GenericConverter.class)) {
            registry.addConverter(converter);
        }
        for (Formatter<?> formatter : getBeansOfType(Formatter.class)) {
            registry.addFormatter(formatter);
        }
    }

    private <T> Collection<T> getBeansOfType(Class<T> type) {
        if (CanalControllerAnnotationBeanPostProcessor.this.beanFactory instanceof ListableBeanFactory) {
            return ((ListableBeanFactory) CanalControllerAnnotationBeanPostProcessor.this.beanFactory)
                    .getBeansOfType(type)
                    .values();
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * An {@link MessageHandlerMethodFactory} adapter that offers a configurable underlying
     * instance to use. Useful if the factory to use is determined once the endpoints
     * have been registered but not created yet.
     *
     * @see KafkaListenerEndpointRegistrar#setMessageHandlerMethodFactory
     */
    private class KafkaHandlerMethodFactoryAdapter implements MessageHandlerMethodFactory {

        private final DefaultFormattingConversionService defaultFormattingConversionService =
                new DefaultFormattingConversionService();

        private MessageHandlerMethodFactory handlerMethodFactory;

        public void setHandlerMethodFactory(MessageHandlerMethodFactory kafkaHandlerMethodFactory1) {
            this.handlerMethodFactory = kafkaHandlerMethodFactory1;
        }

        @Override
        public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
            return getHandlerMethodFactory().createInvocableHandlerMethod(bean, method);
        }

        private MessageHandlerMethodFactory getHandlerMethodFactory() {
            if (this.handlerMethodFactory == null) {
                this.handlerMethodFactory = createDefaultMessageHandlerMethodFactory();
            }
            return this.handlerMethodFactory;
        }

        private MessageHandlerMethodFactory createDefaultMessageHandlerMethodFactory() {
            DefaultMessageHandlerMethodFactory defaultFactory = new DefaultMessageHandlerMethodFactory();
            Validator validator = CanalControllerAnnotationBeanPostProcessor.this.registrar.getValidator();
            if (validator != null) {
                defaultFactory.setValidator(validator);
            }
            defaultFactory.setBeanFactory(CanalControllerAnnotationBeanPostProcessor.this.beanFactory);

            ConfigurableBeanFactory cbf =
                    CanalControllerAnnotationBeanPostProcessor.this.beanFactory instanceof ConfigurableBeanFactory ?
                            (ConfigurableBeanFactory) CanalControllerAnnotationBeanPostProcessor.this.beanFactory :
                            null;


            this.defaultFormattingConversionService.addConverter(
                    new BytesToStringConverter(CanalControllerAnnotationBeanPostProcessor.this.charset));

            defaultFactory.setConversionService(this.defaultFormattingConversionService);

            List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

            // Annotation-based argument resolution
            argumentResolvers.add(new HeaderMethodArgumentResolver(this.defaultFormattingConversionService, cbf));
            argumentResolvers.add(new HeadersMethodArgumentResolver());

            // Type-based argument resolution
            final CanalListSmartConvert messageConverter =
                    new CanalListSmartConvert();

            argumentResolvers.add(new MessageMethodArgumentResolver(messageConverter));
            argumentResolvers.add(new KafkaNullAwarePayloadArgumentResolver(messageConverter, validator));
            defaultFactory.setArgumentResolvers(argumentResolvers);

            defaultFactory.afterPropertiesSet();
            return defaultFactory;
        }

    }

    private class CanalListSmartConvert extends AbstractMessageConverter {
        @Override
        protected boolean supports(Class<?> clazz) {
            return List.class.equals(clazz) || CanalTypeEnum.class.equals(clazz);
        }

        @Override
        protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
            if (List.class.equals(targetClass)) {
                //protect class can't get rawType,just toJson
                return Collections.singletonList(message.getPayload());
            }

            //proxyBean adapt
            return CanalTypeEnum.SELECT;
        }
    }


    private static class BytesToStringConverter implements Converter<byte[], String> {


        private final Charset charset;

        BytesToStringConverter(Charset charset) {
            this.charset = charset;
        }

        @Override
        public String convert(byte[] source) {
            return new String(source, this.charset);
        }

    }

    private static class ListenerScope implements Scope {

        private final Map<String, Object> listeners = new HashMap<>();

        ListenerScope() {
            super();
        }

        public void addListener(String key, Object bean) {
            this.listeners.put(key, bean);
        }

        public void removeListener(String key) {
            this.listeners.remove(key);
        }

        @Override
        public Object get(String name, ObjectFactory<?> objectFactory) {
            return this.listeners.get(name);
        }

        @Override
        public Object remove(String name) {
            return null;
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback) {
        }

        @Override
        public Object resolveContextualObject(String key) {
            return this.listeners.get(key);
        }

        @Override
        public String getConversationId() {
            return null;
        }

    }

    private static class KafkaNullAwarePayloadArgumentResolver extends PayloadMethodArgumentResolver {

        KafkaNullAwarePayloadArgumentResolver(MessageConverter messageConverter, Validator validator) {
            super(messageConverter, validator);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception { // NOSONAR
            Object resolved = super.resolveArgument(parameter, message);
            /*
             * Replace KafkaNull list elements with null.
             */
            if (resolved instanceof List) {
                List<?> list = ((List<?>) resolved);
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof KafkaNull) {
                        list.set(i, null);
                    }
                }
            }
            return resolved;
        }

        @Override
        protected boolean isEmptyPayload(Object payload) {
            return payload == null || payload instanceof KafkaNull;
        }

    }

}
