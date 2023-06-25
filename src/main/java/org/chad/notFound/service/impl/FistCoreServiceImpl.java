package org.chad.notFound.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.configuration.FistProperties;
import org.chad.notFound.constant.FistConstant;
import org.chad.notFound.model.RollBackInfo;
import org.chad.notFound.model.Sql;
import org.chad.notFound.model.SyncInfo;
import org.chad.notFound.model.vo.CallBack;
import org.chad.notFound.service.ICallbackService;
import org.chad.notFound.service.IFistCoreService;
import org.chad.notFound.threadLocal.FistThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.sql.Connection;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private ICallbackService callbackService;
    public static final Map<String, List<Connection>> CONNECTION_MAP = new ConcurrentHashMap<>(16);

    @Autowired
    public void setCallbackService(ICallbackService callbackService) {
        this.callbackService = callbackService;
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
    public void recordSql(List<Sql> sql, Throwable thrown, String group) {
        sql.forEach(Sql::generateRollBackSql);
        RollBackInfo rollBackInfo = FistThreadLocal.ROLLBACK_INFO.get();
        SyncInfo syncInfo = new SyncInfo();
        syncInfo.setRollback(needsRollBack(thrown, rollBackInfo));
        syncInfo.setRollbackSql(sql.stream().map(Sql::getRollBackSql).collect(Collectors.toList()));
        syncInfo.setEnd(FistThreadLocal.BASE.get() == null || FistThreadLocal.BASE.get());
        syncInfo.setFistId(FistThreadLocal.TRACE_ID.get());
        syncInfo.setGroup(group);
        //Now I guess it's time to send the syncInfo to rust server
        asyncSend(syncInfo);
    }

    /**
     * process connection for tcc mode
     *
     * @param conns  connections
     * @param thrown exception
     * @param group  group
     */
    @Override
    public void recordConn(List<Connection> conns, Throwable thrown, String group) {
        CONNECTION_MAP.put(FistThreadLocal.TRACE_ID.get(), conns);
        SyncInfo syncInfo = new SyncInfo();
        syncInfo.setGroup(group)
                .setEnd(FistThreadLocal.BASE.get() == null || FistThreadLocal.BASE.get())
                .setFistId(FistThreadLocal.TRACE_ID.get())
                .setRollback(needsRollBack(thrown, FistThreadLocal.ROLLBACK_INFO.get()));
        asyncSend(syncInfo);
    }

    private static final HttpClient httpClient = HttpClient
            .create(ConnectionProvider
                    .builder("fist-provider")
                    .maxConnections(50)
                    .pendingAcquireMaxCount(-1)
                    .pendingAcquireTimeout(Duration.ofMillis(45000))
                    .build())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
            .disableRetry(false).responseTimeout(Duration.ofMillis(15000))
            .keepAlive(true);

    /**
     * async send the info of transaction to rust server now
     *
     * @param syncInfo syncInfo
     */
    @Override
    public void asyncSend(SyncInfo syncInfo) {
        syncInfo.setFistId(encrypt(syncInfo.getFistId()));
        String fistServerAddr = "http://" + fistProperties.getFistServerAddr() + ":" + fistProperties.getFistServerPort() + FistConstant.FIST_PATH;
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
        httpClient
                .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                .post().uri(fistServerAddr)
                .send(ByteBufFlux.fromString(Mono.just(json)))
                .response()
                .doOnError(Throwable.class, throwable -> {
                    log.error("async send info to fist server error, error: {}", throwable.getMessage());
                    callbackService.ok(new CallBack().setFistId(syncInfo.getFistId()).setGroup(syncInfo.getGroup()));
                })
                .subscribe(response -> log.debug("async send info to fist server success, response: {}", response));
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
