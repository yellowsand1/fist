package org.chad.notFound.service;

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
     * deal with the callback from rust server,saga mode
     *
     * @param callBack callBack
     */
    void dealCallBack(CallBack callBack);

    /**
     * don't need to rollback
     *
     * @param callBack callBack
     */
    void ok(CallBack callBack);

}
