package org.chad.notFound.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.chad.notFound.aop.JdbcConnectionAspect;
import org.chad.notFound.service.IFistAspectService;
import org.chad.notFound.service.IFistCoreService;
import org.chad.notFound.threadLocal.FistThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.service.impl
 * @Author: hyl
 * @CreateTime: 2023-05-04  15:26
 * @Description: fist tcc service impl
 * @Version: 1.0
 */
@Slf4j
public class FistTccServiceImpl implements IFistAspectService {
    private IFistCoreService fistCoreService;

    @Autowired
    public void setFistCoreService(IFistCoreService fistCoreService) {
        this.fistCoreService = fistCoreService;
    }

    /**
     * do on intercept
     *
     * @param pjp   pjp
     * @param group group
     * @return {@link Object}
     */
    @Override
    public Object doOnIntercept(ProceedingJoinPoint pjp, String group) throws Throwable {
        Object res;
        Throwable thrown = null;
        long l = System.currentTimeMillis();
        try {
            FistThreadLocal.IN_TRANSACTION.set(false);
            res = pjp.proceed();
        } catch (Throwable e) {
            thrown = e;
            throw e;
        } finally {
            List<Connection> connections = JdbcConnectionAspect.CONNECTIONS.get();
            JdbcConnectionAspect.CONNECTIONS.remove();
            if (connections == null) {
                FistThreadLocal.IN_TRANSACTION.remove();
                throw new RuntimeException("connection is null,may not support this orm");
            }
            try {
                fistCoreService.recordConn(connections, thrown, group);
            }catch (Exception e){
                log.error("fist tcc doOnIntercept error",e);
                throw new RuntimeException(e);
            }finally {
                FistThreadLocal.IN_TRANSACTION.remove();
            }
            log.debug("GlobalTransactional base on fist ends, cost {} ms", System.currentTimeMillis() - l);
        }
        return res;
    }
}
