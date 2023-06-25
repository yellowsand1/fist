package org.chad.notFound.service.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.lock.FistLock;
import org.chad.notFound.model.RollBackSql;
import org.chad.notFound.model.vo.CallBack;
import org.chad.notFound.service.ICallbackService;
import org.chad.notFound.threadLocal.FistThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.service.impl
 * @Author: hyl
 * @CreateTime: 2023-04-25  09:33
 * @Description: callback service impl
 * @Version: 1.0
 */
@Slf4j
public class SagaCallbackServiceImpl implements ICallbackService {
    private DataSource dataSource;
    private FistLock fistLock;

    @Autowired
    public void setFistLock(FistLock fistLock) {
        this.fistLock = fistLock;
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * deal with the callback from rust server
     *
     * @param callBack callBack
     */
    @Override
    public void dealCallBack(CallBack callBack) {
        try {
            callBack.setFistId(decrypt(callBack.getFistId()));
            for (RollBackSql rollBackSql : callBack.getRollBackSql()) {
                executeRollBack(rollBackSql);
            }
        } finally {
            log.debug("---------------------------unlock-------------------------------------");
            fistLock.unlock(callBack.getGroup());
        }
    }

    /**
     * don't need to rollback
     *
     * @param callBack callBack
     */
    @Override
    public void ok(CallBack callBack) {
        try {
            callBack.setFistId(decrypt(callBack.getFistId()));
        } finally {
            log.debug("---------------------------unlock-------------------------------------");
            fistLock.unlock(callBack.getGroup());
        }
    }

    /**
     * execute the rollback sql
     *
     * @param rollBackSql rollBackSql
     */
    @SneakyThrows
    private void executeRollBack(RollBackSql rollBackSql) {
        FistThreadLocal.ROLLBACK_ING.set("rollback");
        String sql = rollBackSql.getSql();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            conn.setAutoCommit(false);
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            for (List<Object> param : rollBackSql.getParams()) {
                for (int i = 0; i < param.size(); i++) {
                    preparedStatement.setObject(i + 1, param.get(i));
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            conn.rollback();
            throw new RuntimeException("execute rollback sql error", e);
        } finally {
            assert conn != null;
            conn.commit();
            FistThreadLocal.ROLLBACK_ING.remove();
        }
    }
}
