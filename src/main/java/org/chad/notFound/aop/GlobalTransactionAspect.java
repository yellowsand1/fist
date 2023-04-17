package org.chad.notFound.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.chad.notFound.annotation.GlobalTransactional;
import org.chad.notFound.constant.FistConstant;
import org.chad.notFound.model.RollBackInfo;
import org.chad.notFound.model.Sql;
import org.chad.notFound.service.IFistCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: aop
 * @Author: hyl
 * @CreateTime: 2023-04-01  15:13
 * @Description: aop for global transaction annotation
 * @Version: 1.0
 */
@Aspect
@Order(FistConstant.FIST_AOP_ORDER)
@Slf4j
public class GlobalTransactionAspect {
    public static final ThreadLocal<RollBackInfo> ROLL_BACK_THREAD_LOCAL = new ThreadLocal<>();
    public static final ThreadLocal<Boolean> CLOSEABLE = new ThreadLocal<>();
    private IFistCoreService coreService;

    @Autowired
    public void setCoreService(IFistCoreService coreService) {
        this.coreService = coreService;
    }

    /**
     * take the basic rollback info
     * send the info to service to record invoked sql
     * <p>
     * due to avoiding current method throw exception,
     * I can only think of doing my logic in finally block,
     * which is highly not recommended and not elegant
     * <p>
     * DataSourceUtils.getConnection(dataSource) can get the connection that intercepted method using,
     * but what if I found the threadLocal already has the connection? which means that this is not the first
     * time I intercept a method in one request, in theory, Spring aop won't allow this, but I'm going to remain the
     * already existed connection for caution.
     *
     * @param pjp pjp
     * @return {@link Object}
     */
    @Around("@annotation(org.chad.notFound.annotation.GlobalTransactional)")
    public Object handleGlobalTransaction(ProceedingJoinPoint pjp) throws Throwable {
        long l = System.currentTimeMillis();
        log.debug("GlobalTransactional base on fist begins");
        GlobalTransactional annotation = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(GlobalTransactional.class);
        processAnnotation(annotation);
        Object res;
        Throwable thrown = null;
        CLOSEABLE.set(false);
        try {
            res = pjp.proceed();
        } catch (Throwable e) {
            thrown = e;
            throw e;
        } finally {
            List<Connection> connections = JdbcConnectionAspect.CONNECTIONS.get();
            JdbcConnectionAspect.CONNECTIONS.remove();
            if (connections == null) {
                throw new RuntimeException("connection is null,may not support this orm");
            }
            List<Sql> sqlList = JdbcConnectionAspect.SQL_LIST.get();
            if (sqlList != null) {
                coreService.recordSql(sqlList, thrown);
            }
            CLOSEABLE.remove();
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

    /**
     * process the annotation
     * when the threadLocal is not null,it means multiple annotation has been used in one request,
     * but I due to the Spring AOP limitation, only the first annotated method will be intercepted,
     * so I decide to ignore the rollBack info which already been set.
     * But if multiple thread has been created in one request and parallel execute,
     * this threadLocal won't work。InheritableThreadLocal may cause this problem,
     * so decide to separate each thread instead of request to do the transaction.
     *
     * @param annotation 注释
     */
    private void processAnnotation(GlobalTransactional annotation) {
        RollBackInfo alreadyRollBackInfo = ROLL_BACK_THREAD_LOCAL.get();
        if (alreadyRollBackInfo != null) {
            return;
        }
        Class<? extends Throwable>[] noRollbackForClazz = annotation.noRollbackFor();
        Class<? extends Throwable>[] rollbackForClazz = annotation.rollbackFor();
        String[] noRollbackForClassNames = annotation.noRollbackForClassName();
        String[] rollbackForClassNames = annotation.rollbackForClassName();
        List<Class> noRollbackFor = new ArrayList<>(Arrays.asList(noRollbackForClazz));
        List<Class> rollbackFor = new ArrayList<>(Arrays.asList(rollbackForClazz));
        for (String className : noRollbackForClassNames) {
            try {
                noRollbackFor.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                log.error("class not found for name: {}", className);
            }
        }
        for (String className : rollbackForClassNames) {
            try {
                rollbackFor.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                log.error("class not found for name: {}", className);
            }
        }
        RollBackInfo rollBackInfo = new RollBackInfo();
        rollBackInfo.setNoRollbackFor(noRollbackFor);
        rollBackInfo.setRollbackFor(rollbackFor);
        ROLL_BACK_THREAD_LOCAL.set(rollBackInfo);
    }
}