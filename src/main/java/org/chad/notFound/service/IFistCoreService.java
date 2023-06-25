package org.chad.notFound.service;

import org.chad.notFound.model.Sql;
import org.chad.notFound.model.SyncInfo;

import java.sql.Connection;
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
     * @param group  lockGroup
     */
    void recordSql(List<Sql> sql, Throwable thrown,String group);

    /**
     * process connection for tcc mode
     *
     * @param conns  connections
     * @param thrown exception
     * @param group  group
     */
    void recordConn(List<Connection> conns, Throwable thrown,String group);

    /**
     * async send the info of transaction to rust server now
     *
     * @param syncInfo syncInfo
     */
    void asyncSend(SyncInfo syncInfo);

    /**
     * encrypt data transfer to rust server
     *
     * @param data data
     * @return {@link String}
     */
    default String encrypt(String data){
        return data;
    }
}
