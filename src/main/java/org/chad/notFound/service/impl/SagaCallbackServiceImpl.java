package org.chad.notFound.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.lock.FistLock;
import org.chad.notFound.model.RollBackSql;
import org.chad.notFound.model.vo.CallBack;
import org.chad.notFound.service.ICallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

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
        fistLock.unlock(callBack.getGroup());
    }

    public static final ThreadLocal<String> ROLLBACK = new ThreadLocal<>();

    /**
     * execute the rollback sql
     *
     * @param rollBackSql rollBackSql
     */
    private void executeRollBack(RollBackSql rollBackSql) {
        ROLLBACK.set("rollback");
        String sql = rollBackSql.getSql();
        try (Connection conn = DataSourceUtils.getConnection(dataSource)) {
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            for (List<Object> param : rollBackSql.getParams()) {
                for (int i = 0; i < param.size(); i++) {
                    preparedStatement.setObject(i + 1, param.get(i));
                }
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException("execute rollback sql error", e);
        } finally {
            ROLLBACK.remove();
        }
    }
}
