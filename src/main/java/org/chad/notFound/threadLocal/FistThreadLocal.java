package org.chad.notFound.threadLocal;

import org.chad.notFound.model.RollBackInfo;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.threadLocal
 * @Author: hyl
 * @CreateTime: 2023-04-15  14:27
 * @Description: native thread local to record fist id
 * @Version: 1.0
 */
public class FistThreadLocal {
    /**
     * to tell server whether this request is the starter and also the end of this global transaction
     */
    public static final ThreadLocal<Boolean> BASE = new ThreadLocal<>();
    /**
     * store global transaction trace id in this request scope
     */
    public static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    /**
     * to separate rollback connection with normal connection in client,
     * it's been set when generate rollback sql and remove right after
     */
    public static final ThreadLocal<String> CLOSEABLE = new ThreadLocal<>();
    /**
     * it's been set when rollback sql execute ,also to tell jdbc aspect not to intercept
     */
    public static final ThreadLocal<String> ROLLBACK_ING = new ThreadLocal<>();
    /**
     * to store rollbackInfo in this request scope
     */
    public static final ThreadLocal<RollBackInfo> ROLLBACK_INFO = new ThreadLocal<>();
    /**
     * to tell the jdbc aspect not to allow the connection to close,commit,rollback and setAutoCommit
     */
    public static final ThreadLocal<Boolean> IN_TRANSACTION = new ThreadLocal<>();
}
