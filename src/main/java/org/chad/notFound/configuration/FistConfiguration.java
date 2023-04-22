package org.chad.notFound.configuration;

import feign.RequestInterceptor;
import org.chad.notFound.aop.GlobalTransactionAspect;
import org.chad.notFound.aop.JdbcConnectionAspect;
import org.chad.notFound.applicationRunner.DbMetaDataApplicationRunner;
import org.chad.notFound.rpc.consumer.TraceFilter;
import org.chad.notFound.rpc.provider.FistFeignInterceptor;
import org.chad.notFound.rpc.provider.RestTemplateRpcAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

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
}
