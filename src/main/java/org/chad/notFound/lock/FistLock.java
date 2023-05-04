package org.chad.notFound.lock;

/**
 * interface for fist lock, implementation of this interface will be used in @GlobalTransaction services
 *
 * @author hyl
 * @date 2023/05/04
 */
public interface FistLock {
    /**
     * lock
     * needs to be a synchronized method !
     */
    void lock(String group);

    /**
     * unlock
     * can't be a synchronized method due to competition with lock method
     */
    void unlock(String group);

    /**
     * check if locked
     * needs to be synchronized method !
     *
     * @return true if locked
     */
    boolean locked(String group);

    /**
     * retry until unlocked
     *
     * @param group lock group
     */
    default void retry(String group) {
        synchronized (this) {
            long l = System.currentTimeMillis();
            long interval = 1;
            while (locked(group)) {
                try {
                    if (interval > 15000 || System.currentTimeMillis() - l > 15000) {
                        throw new RuntimeException("global lock timeout");
                    }
                    Thread.sleep(interval << 1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
