package org.chad.notFound.controller;

import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.lock.FistLock;
import org.chad.notFound.model.vo.CallBack;
import org.chad.notFound.service.ICallbackService;
import org.chad.notFound.threadFactory.FistThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.controller
 * @Author: hyl
 * @CreateTime: 2023-04-10  14:34
 * @Description: fist controller to accept request from fist server
 * @Version: 1.0
 */
@RestController
@RequestMapping("/fist")
@Slf4j
public class FistController {
    private ICallbackService callbackService;
    private FistLock fistLock;
    public static final Executor rollbackThreadPool = new ThreadPoolExecutor(10, Runtime.getRuntime().availableProcessors() * 2, 15L, TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>(1000), new FistThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    @Autowired
    public void setFistLock(FistLock fistLock) {
        this.fistLock = fistLock;
    }

    @Autowired
    public void setCallbackService(ICallbackService callbackService) {
        this.callbackService = callbackService;
    }

//    @PostMapping("/core")
//    public DeferredResult<ResponseEntity<?>> core(@RequestBody CallBack callBack) {
//        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>();
//        rollbackThreadPool.execute(() -> {
//            try {
//                callbackService.dealCallBack(callBack);
//                deferredResult.setResult(ResponseEntity.ok("fist"));
//            } catch (Exception e) {
//                deferredResult.setErrorResult(e);
//            }
//        });
//        deferredResult.onCompletion(FistGlobalLock::unlock);
//        return deferredResult;
//    }

    @PostMapping("/core")
    @Async("fistCallbackExecutor")
    public CompletableFuture<String> core(@RequestBody CallBack callBack) {
        callbackService.dealCallBack(callBack);
        return CompletableFuture.completedFuture("fist");
    }

    @PostMapping("/ok")
    @Async("fistCallbackExecutor")
    public CompletableFuture<String> ok(@RequestBody CallBack callBack) {
        fistLock.unlock(callBack.getGroup());
        return CompletableFuture.completedFuture("fist");
    }
}