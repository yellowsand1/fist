package org.chad.notFound.sqlReverse;

import org.chad.notFound.model.RollBackSql;
import org.chad.notFound.model.db.MetaData;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.chad.notFound.constant.SqlConstant.*;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.sqlReverse
 * @Author: hyl
 * @CreateTime: 2023-05-23  18:43
 * @Description: rollback sql generator for update sql
 * @Version: 1.0
 */
public class UpdateReverser extends SqlReverser {

    /**
     * generate rollback sql
     *
     * @param sql sql
     * @return {@link String}
     */
    @Override
    public RollBackSql generate(String sql) {
        sql = start(sql);
        RollBackSql rollBackSql = new RollBackSql();
        List<List<Object>> params = new ArrayList<>();
        rollBackSql.setParams(params);
        String tableName = getTableName(sql);
        List<String> primaryKey = MetaData.getPrimaryKey(tableName);
        if (primaryKey == null || primaryKey.isEmpty()) {
            end();
            throw new RuntimeException("Can't find primary key in table "+tableName);
        }
        StringBuilder selectSql = new StringBuilder();
        selectSql.append("select ");
        Map<String, String> allUpdateValues = getAllUpdateValues(sql);
        StringBuilder sb = new StringBuilder();
        sb.append(UPDATE).append(" ").append(tableName).append(" ").append(SET).append(" ");
        sb.append(allUpdateValues.keySet().stream().map(key -> key + " = ?").collect(Collectors.joining(","))).append(" ").append(WHERE).append(" ");
        sb.append(primaryKey.stream().map(key -> key + " = ?").collect(Collectors.joining(AND)));
        rollBackSql.setSql(sb.toString());
        selectSql.append(String.join(",", allUpdateValues.keySet())).append(",").append(String.join(AND, primaryKey)).append(" ").append(FROM).append(" ").append(tableName).append(" ").append(sql.substring(Math.max(sql.indexOf(WHERE), sql.indexOf(WHERE.toUpperCase()))));
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            PreparedStatement ps = conn.prepareStatement(selectSql.toString());
            ResultSet rs = ps.executeQuery();
            DatabaseMetaData metaData = conn.getMetaData();
            Map<String, Integer> columnDataTypes = new HashMap<>(1);
            for (String key : allUpdateValues.keySet()) {
                ResultSet columnsMetaData = metaData.getColumns(null, null, tableName, key);
                if (!columnsMetaData.next()) {
                    throw new RuntimeException("Can't find column " + key + " in table " + tableName);
                }
                int dataType = columnsMetaData.getInt("DATA_TYPE");
                columnDataTypes.put(key, dataType);
            }
            for (String key : primaryKey) {
                ResultSet columnsMetaData = metaData.getColumns(null, null, tableName, key);
                if (!columnsMetaData.next()) {
                    throw new RuntimeException("Can't find column " + key + " in table " + tableName);
                }
                int dataType = columnsMetaData.getInt("DATA_TYPE");
                columnDataTypes.put(key, dataType);
            }
            while (rs.next()) {
                List<Object> param = new ArrayList<>();
                for (String key : allUpdateValues.keySet()) {
                    predicateDataType(rs, columnDataTypes, param, key);
                }
                for (String key : primaryKey) {
                    predicateDataType(rs, columnDataTypes, param, key);
                }
                params.add(param);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            end();
        }
        return rollBackSql;
    }

    /**
     * get table name of this sql
     *
     * @param sql sql
     * @return {@link String}
     */
    @Override
    String getTableName(String sql) {
        int index = Math.max(sql.indexOf(UPDATE), sql.indexOf(UPDATE.toUpperCase()));
        int start = index + UPDATE.length();
        String[] wholeName = skipSpace(sql, start).replaceAll("`", "").split("\\.");
        if (wholeName.length > 1) {
            return wholeName[wholeName.length-1];
        }
        return wholeName[0];
    }

    /**
     * get all update values from a update sql
     *
     * @param sql sql
     * @return {@link Map}<{@link String}, {@link String}>
     */
    private Map<String, String> getAllUpdateValues(String sql) {
        Map<String, String> map = new HashMap<>();
        int index = Math.max(sql.indexOf(SET), sql.indexOf(SET.toUpperCase()));
        int start = index + SET.length() + 1;
        int end = Math.max(sql.indexOf(WHERE), sql.indexOf(WHERE.toUpperCase()));
        String[] values = sql.substring(start, end).split(",");
        for (String value : values) {
            String[] kv = value.split("=");
            map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }
}
