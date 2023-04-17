package org.chad.notFound.rpc;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.aop.rpc
 * @Author: hyl
 * @CreateTime: 2023-04-10  13:49
 * @Description: Implement the aspect of dubbo rpc
 * @Version: 1.0
 */
@Aspect
@Deprecated
public class DubboRpcAspect {
    @Around("execution(* com.alibaba.dubbo.rpc.protocol.dubbo.DubboInvoker.doInvoke(..))")
    public Object intercept(ProceedingJoinPoint joinPoint) throws Throwable {
        //todo!
        return joinPoint.proceed();
    }
}