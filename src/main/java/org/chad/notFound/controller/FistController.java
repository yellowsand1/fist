package org.chad.notFound.controller;

import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.model.vo.CallBack;
import org.chad.notFound.service.ICallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

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

    @Autowired
    public void setCallbackService(ICallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @PostMapping("/core")
    @Async("fistCallbackExecutor")
    public CompletableFuture<String> saga(@RequestBody CallBack callBack) {
        callbackService.dealCallBack(callBack);
        return CompletableFuture.completedFuture("fist");
    }


    @PostMapping("/ok")
    @Async("fistCallbackExecutor")
    public CompletableFuture<String> ok(@RequestBody CallBack callBack) {
        callbackService.ok(callBack);
        return CompletableFuture.completedFuture("fist");
    }
}