package org.chad.notFound.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model
 * @Author: hyl
 * @CreateTime: 2023-04-01  17:13
 * @Description: classes for rollback n noRollback according to annotation
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode
@Accessors(chain = true)
public class RollBackInfo implements Serializable {
    private static final long serialVersionUID = 628674163228343231L;
    private List<Class> rollbackFor;
    private List<Class> noRollbackFor;
}
