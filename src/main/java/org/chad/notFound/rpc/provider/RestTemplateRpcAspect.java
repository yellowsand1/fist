package org.chad.notFound.rpc.provider;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.chad.notFound.threadLocal.FistThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.aop
 * @Author: hyl
 * @CreateTime: 2023-04-10  10:32
 * @Description: Finally, I had to choose the stupidest way to implement trace rpc request
 * between microservices,that is to implement a aspect for every rpc framework and protocol.Let's pray that rpc protocol
 * has a mutual standard in the future.=-=
 * @Version: 1.0
 */
@Aspect
public class RestTemplateRpcAspect {
    private RestTemplate restTemplate;

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Around("execution(* org.springframework.web.client.RestTemplate.getForObject(..)) || " +
            "execution(* org.springframework.web.client.RestTemplate.postForObject(..)) || " +
            "execution(* org.springframework.web.client.RestTemplate.postForEntity(..)) || " +
            "execution(* org.springframework.web.client.RestTemplate.getForEntity(..))")
    public Object intercept(ProceedingJoinPoint joinPoint) throws Throwable {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            headers.add("fist", FistThreadLocal.TRACE_ID.get());
            return execution.execute(request, body);
        });
        return joinPoint.proceed();
    }
}
