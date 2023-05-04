package org.chad.notFound.service;

import org.chad.notFound.model.RollBackSql;
import org.chad.notFound.model.vo.CallBack;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.service
 * @Author: hyl
 * @CreateTime: 2023-04-25  09:33
 * @Description: service for callback
 * @Version: 1.0
 */
public interface ICallbackService {
    /**
     * deal with the callback from rust server
     *
     * @param callBack callBack
     */
    void dealCallBack(CallBack callBack);

    /**
     * execute the rollback sql
     *
     * @param rollBackSql rollBackSql
     */
    void executeRollBack(RollBackSql rollBackSql);
}
