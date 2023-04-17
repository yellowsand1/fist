package org.chad.notFound.service;

import org.chad.notFound.model.RollBackSql;
import org.chad.notFound.model.Sql;
import org.chad.notFound.model.SyncInfo;
import org.chad.notFound.model.vo.CallBack;

import java.util.List;

/**
 * core service
 *
 * @author hyl
 * @date 2023/04/01
 */
public interface IFistCoreService {

    /**
     * process sql
     *
     * @param sql    sql
     * @param thrown exception
     */
    void recordSql(List<Sql> sql, Throwable thrown);

    /**
     * send the info of transaction to rust server now
     *
     * @param syncInfo syncInfo
     */
    void send(SyncInfo syncInfo);

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
