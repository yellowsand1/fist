package org.chad.notFound.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.lock
 * @Author: hyl
 * @CreateTime: 2023-04-25  10:19
 * @Description: global lock to prevent read dirty data in concurrency,
 * but this is just a default implementation that will only lock single node
 * @Version: 1.0
 */
public class FistGlobalLock implements FistLock {
    private static final Map<String, AtomicBoolean> LOCK_MAP = new ConcurrentHashMap<>(16);

    public synchronized void lock(String group) {
        AtomicBoolean lock = LOCK_MAP.get(group);
        if (lock == null) {
            lock = new AtomicBoolean(false);
            LOCK_MAP.put(group, lock);
        }
        retry(group);
        lock.set(true);
    }

    public void unlock(String group) {
        AtomicBoolean lock = LOCK_MAP.get(group);
        if (lock == null) {
            return;
        }
        lock.set(false);
    }

    public synchronized boolean locked(String group) {
        AtomicBoolean lock = LOCK_MAP.get(group);
        if (lock == null) {
            return false;
        }
        return lock.get();
    }
}
