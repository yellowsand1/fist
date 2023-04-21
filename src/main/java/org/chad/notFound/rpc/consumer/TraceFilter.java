package org.chad.notFound.rpc.consumer;

import org.chad.notFound.aop.GlobalTransactionAspect;
import org.chad.notFound.aop.JdbcConnectionAspect;
import org.chad.notFound.constant.FistConstant;
import org.chad.notFound.threadLocal.FistThreadLocal;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.filter
 * @Author: hyl
 * @CreateTime: 2023-04-04  13:41
 * @Description: generate or get trace id,to judge whether it's a request from another distributed server
 * or a request from a client.But I don't know when to remove the trace id from the threadLocal, or
 * maybe I don't need a threadLocal,just to add one when filter this request and never remove it,
 * which sounds more reasonable.
 * When there's no traceId in the request,I assume that it's the start of a new request from a client,
 * and when the aop method run over, it means the whole transaction's over!
 * @Version: 1.0
 */
@Order(FistConstant.FIST_FILTER_ORDER)
public class TraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getHeader("fist") == null) {
            FistThreadLocal.TRACE_ID.set(UUID.randomUUID().toString());
            FistThreadLocal.BASE.set(true);
        } else {
            FistThreadLocal.TRACE_ID.set(request.getHeader("fist"));
            FistThreadLocal.BASE.set(false);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            FistThreadLocal.BASE.remove();
            FistThreadLocal.TRACE_ID.remove();
            //remove the other two threadLocal after every request
            //regardless of whether the request is a base request or not
            //because I send everything I need to the rust server
            GlobalTransactionAspect.ROLL_BACK_THREAD_LOCAL.remove();
            JdbcConnectionAspect.SQL_LIST.remove();
        }
    }
}
