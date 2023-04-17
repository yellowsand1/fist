package org.chad.notFound.model.db;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model
 * @Author: hyl
 * @CreateTime: 2023-04-04  14:16
 * @Description: Base column information
 * @Version: 1.0
 */
@Data
@Accessors(chain = true)
public class Column {
    private String columnName;
}
