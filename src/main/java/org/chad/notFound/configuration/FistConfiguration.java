package org.chad.notFound.configuration;

import feign.RequestInterceptor;
import org.chad.notFound.aop.GlobalTransactionAspect;
import org.chad.notFound.aop.JdbcConnectionAspect;
import org.chad.notFound.applicationRunner.DbMetaDataApplicationRunner;
import org.chad.notFound.lock.FistGlobalLock;
import org.chad.notFound.lock.FistLock;
import org.chad.notFound.rpc.consumer.TraceFilter;
import org.chad.notFound.rpc.provider.FistFeignInterceptor;
import org.chad.notFound.rpc.provider.RestTemplateRpcAspect;
import org.chad.notFound.service.ICallbackService;
import org.chad.notFound.service.IFistAspectService;
import org.chad.notFound.service.impl.FistSagaServiceImpl;
import org.chad.notFound.service.impl.FistTccServiceImpl;
import org.chad.notFound.service.impl.SagaCallbackServiceImpl;
import org.chad.notFound.service.impl.TccCallbackServiceImpl;
import org.chad.notFound.threadFactory.FistThreadFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: configuration
 * @Author: hyl
 * @CreateTime: 2023-04-01  15:07
 * @Description: configurations for fist-java-client
 * @Version: 1.0
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableAsync
@ComponentScan(basePackages = {"org.chad.notFound"})
public class FistConfiguration {
    @Bean
    public GlobalTransactionAspect globalTransactionAspect() {
        return new GlobalTransactionAspect();
    }

    @Bean
    public FistProperties fistProperties() {
        return new FistProperties();
    }

    @Bean
    public FistSpringContextHolder fistSpringContextHolder() {
        return new FistSpringContextHolder();
    }

    @Bean
    public JdbcConnectionAspect jdbcConnectionAspect() {
        return new JdbcConnectionAspect();
    }

    @Bean
    public TraceFilter traceFilter() {
        return new TraceFilter();
    }

    @Bean
    public DbMetaDataApplicationRunner dbMetaDataApplicationRunner() {
        return new DbMetaDataApplicationRunner();
    }

    @Bean
    @ConditionalOnProperty(name = "fist.rpc.enable.restTemplate.", havingValue = "true")
    public RestTemplateRpcAspect restTemplateRpcAspect() {
        return new RestTemplateRpcAspect();
    }

    @Bean
    @ConditionalOnProperty(name = "fist.rpc.enable.feign", havingValue = "true")
    public RequestInterceptor fistFeignInterceptor() {
        return new FistFeignInterceptor();
    }

    @Bean("fistCallbackExecutor")
    public Executor fistCallbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadFactory(new FistThreadFactory());
        executor.setKeepAliveSeconds(60);
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public FistLock fistLock() {
        return new FistGlobalLock();
    }

    @Bean
    @ConditionalOnProperty(name = "fist.mode", havingValue = "saga")
    public IFistAspectService fistSagaAspectService() {
        return new FistSagaServiceImpl();
    }

    @Bean
    @ConditionalOnProperty(name = "fist.mode", havingValue = "tcc")
    public IFistAspectService fistTccAspectService() {
        return new FistTccServiceImpl();
    }

    @Bean
    @ConditionalOnProperty(name = "fist.mode", havingValue = "saga")
    public ICallbackService callbackSagaService(){
        return new SagaCallbackServiceImpl();
    }

    @Bean
    @ConditionalOnProperty(name = "fist.mode", havingValue = "tcc")
    public ICallbackService callbackTccService(){
        return new TccCallbackServiceImpl();
    }
}
