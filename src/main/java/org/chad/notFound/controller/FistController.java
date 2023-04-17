package org.chad.notFound.controller;

import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.model.vo.CallBack;
import org.chad.notFound.service.IFistCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private IFistCoreService coreService;

    @Autowired
    public void setCoreService(IFistCoreService coreService) {
        this.coreService = coreService;
    }

    @PostMapping("/core")
    public String core(@RequestBody CallBack callBack) {
        coreService.dealCallBack(callBack);
        return "fist";
    }
}