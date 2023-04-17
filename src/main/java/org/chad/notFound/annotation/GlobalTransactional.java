package org.chad.notFound.annotation;

import java.lang.annotation.*;

/**
 * GlobalTransactional annotation for method
 *
 * @author hyl
 * @date 2023/04/01
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GlobalTransactional {
    /**
     * transaction name
     *
     * @return
     */
    String name() default "";

    /**
     * timeout
     *
     * @return
     */
    int timeout() default 60000;

    /**
     * rollback for
     *
     * @return
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * rollback for class name
     *
     * @return
     */
    String[] rollbackForClassName() default {};

    /**
     * no rollback for
     *
     * @return
     */
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * no rollback for class name
     *
     * @return
     */
    String[] noRollbackForClassName() default {};
}
