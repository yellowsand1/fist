package org.chad.notFound.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.chad.notFound.aop.JdbcConnectionAspect;
import org.chad.notFound.lock.FistLock;
import org.chad.notFound.model.Sql;
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
 * @CreateTime: 2023-05-04  15:27
 * @Description: fist saga service impl
 * @Version: 1.0
 */
@Slf4j
public class FistSagaServiceImpl implements IFistAspectService {
    private IFistCoreService coreService;
    private FistLock fistLock;

    @Autowired
    public void setFistLock(FistLock fistLock) {
        this.fistLock = fistLock;
    }

    @Autowired
    public void setCoreService(IFistCoreService coreService) {
        this.coreService = coreService;
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
            fistLock.lock(group);
            log.debug("---------------------------lock-------------------------------------");
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
            List<Sql> sqlList = JdbcConnectionAspect.SQL_LIST.get();
            if (sqlList != null) {
                try {
                    coreService.recordSql(sqlList, thrown, group);
                } catch (Exception e) {
                    log.error("fist saga error", e);
                    throw new RuntimeException(e);
                } finally {
                    FistThreadLocal.IN_TRANSACTION.remove();
                }
            } else {
                log.debug("---------------------------unlock-------------------------------------");
                fistLock.unlock(group);
            }
            FistThreadLocal.IN_TRANSACTION.remove();
            for (Connection connection : connections) {
                if (connection != null) {
                    connection.commit();
                    connection.close();
                }
            }
            log.debug("GlobalTransactional base on fist ends, cost {} ms", System.currentTimeMillis() - l);
        }
        return res;
    }
}
