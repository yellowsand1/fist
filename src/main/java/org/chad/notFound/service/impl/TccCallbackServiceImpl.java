package org.chad.notFound.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.model.vo.CallBack;
import org.chad.notFound.service.ICallbackService;
import org.chad.notFound.threadLocal.FistThreadLocal;

import java.sql.Connection;
import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.service.impl
 * @Author: hyl
 * @CreateTime: 2023-05-04  16:43
 * @Description: tcc mode callback service impl
 * @Version: 1.0
 */
@Slf4j
public class TccCallbackServiceImpl implements ICallbackService {
    /**
     * deal with the callback from rust server,saga mode
     *
     * @param callBack callBack
     */
    @Override
    public void dealCallBack(CallBack callBack) {
        executeCallback(callBack.getFistId(), true);
    }

    /**
     * don't need to rollback
     *
     * @param callBack callBack
     */
    @Override
    public void ok(CallBack callBack) {
        executeCallback(callBack.getFistId(), false);
    }

    private void executeCallback(String fistId, boolean rollback) {
        FistThreadLocal.IN_TRANSACTION.set(false);
        List<Connection> connections = FistCoreServiceImpl.CONNECTION_MAP.get(fistId);
        for (Connection connection : connections) {
            try {
                if (connection != null) {
                    if (rollback) {
                        connection.rollback();
                    } else {
                        connection.commit();
                    }
                    connection.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                FistCoreServiceImpl.CONNECTION_MAP.remove(fistId);
                FistThreadLocal.IN_TRANSACTION.remove();
            }
        }
    }
}
