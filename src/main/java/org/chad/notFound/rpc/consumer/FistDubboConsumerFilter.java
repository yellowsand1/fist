package org.chad.notFound.rpc.consumer;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.chad.notFound.threadLocal.FistThreadLocal;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.rpc.consumer
 * @Author: hyl
 * @CreateTime: 2023-04-15  14:25
 * @Description: filter for dubbo rpc
 * @Version: 1.0
 */
@Activate(CommonConstants.CONSUMER)
public class FistDubboConsumerFilter implements Filter {
    /**
     * get trace id to the request
     *
     * @param invoker    invoker
     * @param invocation invocation
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String fistId = invocation.getAttachment("fist");
        if (fistId != null) {
            FistThreadLocal.TRACE_ID.set(fistId);
        }
        return invoker.invoke(invocation);
    }
}
