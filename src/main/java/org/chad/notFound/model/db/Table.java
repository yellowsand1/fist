package org.chad.notFound.model.db;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model
 * @Author: hyl
 * @CreateTime: 2023-04-04  14:16
 * @Description: table structure,consider multiple primary keys,using list
 * @Version: 1.0
 */
@Data
@Accessors(chain = true)
public class Table implements Serializable {
    private static final long serialVersionUID = -7017380568046336672L;
    private String tableName;
    private List<Column> columnList;
    private List<String> primaryKeyList;
}
