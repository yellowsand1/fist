package org.chad.notFound.threadFactory;

import java.util.concurrent.ThreadFactory;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.threadFactory
 * @Author: hyl
 * @CreateTime: 2023-04-23  18:07
 * @Description: fist thread factory
 * @Version: 1.0
 */
public class FistThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("fist-thread");
        thread.setDaemon(true);
        return thread;
    }
}
