package org.chad.notFound.applicationRunner;

import lombok.extern.slf4j.Slf4j;
import org.chad.notFound.configuration.FistProperties;
import org.chad.notFound.constant.FistConstant;
import org.chad.notFound.model.db.Column;
import org.chad.notFound.model.db.MetaData;
import org.chad.notFound.model.db.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

import java.sql.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.applicationRunner
 * @Author: hyl
 * @CreateTime: 2023-04-04  14:09
 * @Description: initialize database metadata for rollback sql generate
 * @Version: 1.0
 */

@Order(FistConstant.FIST_RUNNER_ORDER)
@Slf4j
public class DbMetaDataApplicationRunner implements ApplicationRunner {
    private FistProperties fistProperties;

    @Autowired
    public void setFistProperties(FistProperties fistProperties) {
        this.fistProperties = fistProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long l = System.currentTimeMillis();
        log.debug("fist start to run !");
        String url = fistProperties.getFistTargetDatabaseUrl();
        String username = fistProperties.getFistTargetDatabaseUsername();
        String password = fistProperties.getFistTargetDatabasePassword();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(getTableNameFromUrl(), null, null, new String[]{"TABLE"});
            while (tables.next()) {
                Table table = new Table();
                String tableName = tables.getString("TABLE_NAME");
                ResultSet columns = metaData.getColumns(null, null, tableName, null);
                table.setTableName(tableName.toLowerCase());
                table.setColumnList(new CopyOnWriteArrayList<>());
                table.setPrimaryKeyList(new CopyOnWriteArrayList<>());
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    columns.getInt("DATA_TYPE");
                    Column column = new Column();
                    column.setColumnName(columnName.toLowerCase());
                    table.getColumnList().add(column);
                }
                ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, tableName);
                while (primaryKeys.next()) {
                    String primaryKey = primaryKeys.getString("COLUMN_NAME");
                    table.getPrimaryKeyList().add(primaryKey.toLowerCase());
                }
                MetaData.tableList.add(table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            log.debug("fist configuration finish , costs {} ms", System.currentTimeMillis() - l);
        }
    }
    private String getTableNameFromUrl(){
        String url = fistProperties.getFistTargetDatabaseUrl();
        String[] split = url.split("/");
        String last = split[3];
        String[] split1 = last.split("\\?");
        return split1[0];
    }
}
