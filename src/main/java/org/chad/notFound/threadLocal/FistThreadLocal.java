package org.chad.notFound.threadLocal;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.threadLocal
 * @Author: hyl
 * @CreateTime: 2023-04-15  14:27
 * @Description: native thread local to record fist id
 * @Version: 1.0
 */
public class FistThreadLocal {
    public static final ThreadLocal<Boolean> BASE = new ThreadLocal<>();
    public static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
}
