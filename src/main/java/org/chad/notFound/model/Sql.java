package org.chad.notFound.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.chad.notFound.kit.SqlKit;
import org.chad.notFound.model.enums.SqlType;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.model
 * @Author: hyl
 * @CreateTime: 2023-04-01  16:08
 * @Description: sql information entity
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode
@Accessors(chain = true)
public class Sql {
    private SqlType sqlType;
    private String sql;
    private RollBackSql rollBackSql;

    public Sql(String sql) {
        this.sql = sql.trim();
        switch (this.sql.substring(0, 6).toUpperCase()) {
            case "INSERT":
                this.sqlType = SqlType.INSERT;
                break;
            case "UPDATE":
                this.sqlType = SqlType.UPDATE;
                break;
            case "DELETE":
                this.sqlType = SqlType.DELETE;
                break;
            default:
                this.sqlType = SqlType.SELECT;
                break;
        }
    }

    /**
     * generate rollback sql
     */
    public void generateRollBackSql() {
        switch (this.sqlType) {
            case INSERT:
                this.rollBackSql = SqlKit.rollbackInsert(this.sql);
                break;
            case UPDATE:
                this.rollBackSql = SqlKit.rollbackUpdate(this.sql);
                break;
            case DELETE:
                this.rollBackSql = SqlKit.rollbackDelete(this.sql);
                break;
            case SELECT:
                break;
            default:
                throw new RuntimeException("Unknown Sql Type !");
        }
    }
}
