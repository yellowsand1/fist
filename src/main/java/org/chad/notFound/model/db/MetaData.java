package org.chad.notFound.model.db;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model
 * @Author: hyl
 * @CreateTime: 2023-04-04  14:17
 * @Description: Base inform of all tables
 * @Version: 1.0
 */
@Data
@Accessors(chain = true)
public class MetaData {
    public static volatile CopyOnWriteArrayList<Table> tableList = new CopyOnWriteArrayList<>();

    public static List<String> getPrimaryKey(String tableName) {
        for (Table table : tableList) {
            if (table.getTableName().equals(tableName)) {
                return table.getPrimaryKeyList().stream().distinct().collect(Collectors.toList());
            }
        }
        return null;
    }
}
