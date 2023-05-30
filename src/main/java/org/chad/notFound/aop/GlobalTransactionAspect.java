package org.chad.notFound.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.chad.notFound.annotation.GlobalTransactional;
import org.chad.notFound.constant.FistConstant;
import org.chad.notFound.lock.FistLock;
import org.chad.notFound.model.RollBackInfo;
import org.chad.notFound.service.IFistAspectService;
import org.chad.notFound.threadLocal.FistThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

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
    private IFistAspectService fistAspectService;
    private FistLock fistLock;

    @Autowired
    public void setFistLock(FistLock fistLock) {
        this.fistLock = fistLock;
    }

    @Autowired
    public void setFistAspectService(IFistAspectService fistAspectService) {
        this.fistAspectService = fistAspectService;
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
        log.debug("GlobalTransactional base on fist begins");
        GlobalTransactional annotation = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(GlobalTransactional.class);
        String group = processAnnotation(annotation);
        Object o;
        try {
            o = fistAspectService.doOnIntercept(pjp, group);
        } catch (Exception e) {
            fistLock.unlock(group);
            log.debug("---------------------------lock-------------------------------------");
            log.error("Fist process err, err: {}", e.getMessage());
            throw e;
        }
        return o;
    }

    /**
     * process the annotation
     * when the threadLocal is not null,it means multiple annotation has been used in one request,
     * but I due to the Spring AOP limitation, only the first annotated method will be intercepted,
     * so I decide to ignore the rollBack info which already been set.
     * But if multiple thread has been created in one request and parallel execute,
     * this threadLocal won't work。InheritableThreadLocal may solve this problem,
     * so decide to separate each thread instead of request to do the transaction.
     *
     * @param annotation annotation
     */
    private String processAnnotation(GlobalTransactional annotation) {
        RollBackInfo alreadyRollBackInfo = FistThreadLocal.ROLLBACK_INFO.get();
        if (alreadyRollBackInfo != null) {
            return "";
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
        FistThreadLocal.ROLLBACK_INFO.set(rollBackInfo);
        return annotation.group();
    }
}