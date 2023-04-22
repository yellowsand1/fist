package org.chad.notFound.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.aop.GlobalTransactionAspect;
import org.chad.notFound.configuration.FistProperties;
import org.chad.notFound.constant.FistConstant;
import org.chad.notFound.model.RollBackInfo;
import org.chad.notFound.model.RollBackSql;
import org.chad.notFound.model.Sql;
import org.chad.notFound.model.SyncInfo;
import org.chad.notFound.model.vo.CallBack;
import org.chad.notFound.service.IFistCoreService;
import org.chad.notFound.threadLocal.FistThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.service.impl
 * @Author: hyl
 * @CreateTime: 2023-04-01  15:23
 * @Description: core service impl
 * @Version: 1.0
 */
@Service
@Slf4j
public class FistCoreServiceImpl implements IFistCoreService {
    private FistProperties fistProperties;
    private DataSource dataSource;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Autowired
    public void setFistProperties(FistProperties fistProperties) {
        this.fistProperties = fistProperties;
    }

    /**
     * process sql
     *
     * @param sql    sql
     * @param thrown exception
     */
    @Override
    public void recordSql(List<Sql> sql, Throwable thrown) {
        sql.forEach(Sql::generateRollBackSql);
        RollBackInfo rollBackInfo = GlobalTransactionAspect.ROLL_BACK_THREAD_LOCAL.get();
        SyncInfo syncInfo = new SyncInfo();
        syncInfo.setRollback(needsRollBack(thrown, rollBackInfo));
        syncInfo.setRollbackSql(sql.stream().map(Sql::getRollBackSql).collect(Collectors.toList()));
        syncInfo.setEnd(FistThreadLocal.BASE.get() == null || FistThreadLocal.BASE.get());
        syncInfo.setFistId(FistThreadLocal.TRACE_ID.get());
        //Now I guess it's time to send the syncInfo to rust server
        asyncSend(syncInfo);
    }

    /**
     * deal with the callback from rust server
     *
     * @param callBack callBack
     */
    @Async
    @Override
    public CompletableFuture<Void> dealCallBack(CallBack callBack) {
        long l = System.currentTimeMillis();
        log.debug("roll back demand from fist server,fistId: {}", callBack.getFistId());
        for (RollBackSql rollBackSql : callBack.getRollBackSql()) {
            executeRollBack(rollBackSql);
        }
        log.debug("roll back success,cost: {} ms", System.currentTimeMillis() - l);
        return CompletableFuture.completedFuture(null);
    }

    public static final ThreadLocal<String> ROLLBACK = new ThreadLocal<>();

    /**
     * execute the rollback sql
     *
     * @param rollBackSql rollBackSql
     */
    @Override
    public void executeRollBack(RollBackSql rollBackSql) {
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

    /**
     * async send the info of transaction to rust server now
     *
     * @param syncInfo syncInfo
     */
    @Override
    public void asyncSend(SyncInfo syncInfo) {
        String fistServerAddr = "http://" + fistProperties.getFistServerAddr() + ":" + fistProperties.getFistServerPort() + FistConstant.FIST_SERVER_PATH;
        HttpHeaders headers = getHeaders();
        Map<String, SyncInfo> body = new HashMap<>(1);
        body.put("syncInfo", syncInfo);
        JsonMapper mapper = new JsonMapper();
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        HttpClient.create()
                .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                .post()
                .uri(fistServerAddr)
                .send(ByteBufFlux.fromString(Mono.just(json)))
                .responseContent()
                .aggregate()
                .asString()
                .subscribe(response -> {
                    log.debug("async send info to fist server success, response: {}", response);
                });
    }

    /**
     * form http headers with rest api
     *
     * @return {@link HttpHeaders}
     */
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("content-type", "application/json; charset=utf-8");
        return headers;
    }

    private boolean needsRollBack(Throwable thrown, RollBackInfo rollBackInfo) {
        if (thrown == null) {
            return false;
        }
        return rollBackInfo.getRollbackFor().stream().anyMatch(e -> e.isAssignableFrom(thrown.getClass())) && rollBackInfo.getNoRollbackFor().stream().noneMatch(e -> e.isAssignableFrom(thrown.getClass()));
    }
}
