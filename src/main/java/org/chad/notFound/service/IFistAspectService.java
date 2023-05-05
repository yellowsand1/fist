package org.chad.notFound.service;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * fist aspect service to handle global transaction
 *
 * @author hyl
 * @date 2023/05/04
 */
public interface IFistAspectService {
    /**
     * do on intercept
     *
     * @param pjp   pjp
     * @param group group
     * @return {@link Object}
     */
    Object doOnIntercept(ProceedingJoinPoint pjp,String group) throws Throwable;
}
