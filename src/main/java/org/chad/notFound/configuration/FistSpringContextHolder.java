package org.chad.notFound.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringValueResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * spring context holder
 *
 * @author hyl
 * @date 2023/04/04
 */
public class FistSpringContextHolder implements ApplicationContextAware, EmbeddedValueResolverAware {
    private static ApplicationContext applicationContext = null;
    private static StringValueResolver stringValueResolver;

    public FistSpringContextHolder() {
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        FistSpringContextHolder.applicationContext = applicationContext;
    }

    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        return applicationContext.getBeansWithAnnotation(annotationType);
    }

    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    public static Object getBean(String s) throws BeansException {
        return applicationContext.getBean(s);
    }

    public static <T> T getBean(String s, Class<T> aClass) throws BeansException {
        return applicationContext.getBean(s, aClass);
    }

    public static Object getBean(String s, Object... objects) throws BeansException {
        return applicationContext.getBean(s, objects);
    }

    public static <T> T getBean(Class<T> aClass) throws BeansException {
        return applicationContext.getBean(aClass);
    }

    public static <T> T getBean(Class<T> aClass, Object... objects) throws BeansException {
        return applicationContext.getBean(aClass, objects);
    }

    public static <T> ObjectProvider<T> getBeanProvider(Class<T> aClass) {
        return applicationContext.getBeanProvider(aClass);
    }

    public static <T> ObjectProvider<T> getBeanProvider(ResolvableType resolvableType) {
        return applicationContext.getBeanProvider(resolvableType);
    }

    public static boolean isSingleton(String s) throws NoSuchBeanDefinitionException {
        return applicationContext.isSingleton(s);
    }

    public static boolean isPrototype(String s) throws NoSuchBeanDefinitionException {
        return applicationContext.isPrototype(s);
    }

    public static boolean isTypeMatch(String s, ResolvableType resolvableType) throws NoSuchBeanDefinitionException {
        return applicationContext.isTypeMatch(s, resolvableType);
    }

    public static boolean isTypeMatch(String s, Class<?> aClass) throws NoSuchBeanDefinitionException {
        return applicationContext.isTypeMatch(s, aClass);
    }

    public static Class<?> getType(String s) throws NoSuchBeanDefinitionException {
        return applicationContext.getType(s);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> baseType) {
        return applicationContext.getBeansOfType(baseType);
    }

    public static String[] getAliases(String s) {
        return applicationContext.getAliases(s);
    }

    public static void registerBean(String beanName, String fullClassName) {
        try {
            registerBean(beanName, Class.forName(fullClassName));
        } catch (ClassNotFoundException var3) {
            var3.printStackTrace();
        }

    }

    public static void registerBean(String beanName, Class cla) {
        registerBean(beanName, genericBeanDefinition(cla));
    }

    private static BeanDefinitionBuilder genericBeanDefinition(Class cla) {
        return BeanDefinitionBuilder.genericBeanDefinition(cla);
    }

    private static BeanDefinitionBuilder genericBeanDefinition(String fullClassName) {
        return BeanDefinitionBuilder.genericBeanDefinition(fullClassName);
    }

    public static void registerBean(String beanName, BeanDefinitionBuilder beanDefinitionBuilder) {
        DefaultListableBeanFactory defaultListableBeanFactory = getBeanFactory();
        if (defaultListableBeanFactory.containsBeanDefinition(beanName)) {
            defaultListableBeanFactory.removeBeanDefinition(beanName);
        }

        defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
    }

    public static void registerBean(String beanName, AbstractBeanDefinition beanDefinition) {
        getBeanFactory().registerBeanDefinition(beanName, beanDefinition);
    }

    private static DefaultListableBeanFactory getBeanFactory() {
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
        return defaultListableBeanFactory;
    }

    public static HttpServletRequest request() {
        return requestAttributes().getRequest();
    }

    public static HttpSession session() {
        return request().getSession();
    }

    public static HttpServletResponse response() {
        return requestAttributes().getResponse();
    }

    public static ServletRequestAttributes requestAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }

    public static String webRootDir() {
        return request().getSession().getServletContext().getRealPath("/");
    }

    public static String contextPath() {
        return request().getContextPath();
    }

    public static String serverRootPath() {
        return getSchemeAndHost() + contextPath().replace("/", "");
    }

    public static String getPropertiesValue(String name) {
        try {
            name = "${" + name + "}";
            return stringValueResolver.resolveStringValue(name);
        } catch (Exception var2) {
            return null;
        }
    }

    public void setEmbeddedValueResolver(StringValueResolver stringValueResolver) {
        FistSpringContextHolder.stringValueResolver = stringValueResolver;
    }

    public static String getSchemeAndHost() {
        String origin = request().getHeader("Origin");
        if (origin == null || origin.trim().length() == 0) {
            String requestURL = request().getRequestURL().toString();
            String requestURI = request().getRequestURI();
            return requestURL.replace(requestURI, "/");
        } else {
            return origin + "/";
        }
    }
}
