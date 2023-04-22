package org.chad.notFound.rpc.provider;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.chad.notFound.threadLocal.FistThreadLocal;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.rpc.provider
 * @Author: hyl
 * @CreateTime: 2023-04-22  11:37
 * @Description: feign only needs to add the header on provider side because it's base on http
 * @Version: 1.0
 */
public class FistFeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (FistThreadLocal.TRACE_ID.get() != null) {
            requestTemplate.header("fist", FistThreadLocal.TRACE_ID.get());
        }
    }
}
