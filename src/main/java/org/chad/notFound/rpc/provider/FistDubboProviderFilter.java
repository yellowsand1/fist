package org.chad.notFound.rpc.provider;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.chad.notFound.aop.GlobalTransactionAspect;
import org.chad.notFound.aop.JdbcConnectionAspect;
import org.chad.notFound.threadLocal.FistThreadLocal;

import java.util.UUID;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.rpc
 * @Author: hyl
 * @CreateTime: 2023-04-15  14:08
 * @Description: filter for dubbo rpc
 * @Version: 1.0
 */
@Activate(CommonConstants.PROVIDER)
public class FistDubboProviderFilter implements Filter {
    /**
     * add trace id to the request
     *
     * @param invoker    invoker
     * @param invocation invocation
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        invocation.getAttachments().put("fist", FistThreadLocal.TRACE_ID.get() == null ? UUID.randomUUID().toString() : FistThreadLocal.TRACE_ID.get());
        FistThreadLocal.TRACE_ID.remove();
        FistThreadLocal.BASE.remove();
        GlobalTransactionAspect.ROLL_BACK_THREAD_LOCAL.remove();
        JdbcConnectionAspect.SQL_LIST.remove();
        return invoker.invoke(invocation);
    }
}
