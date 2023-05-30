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
 * @CreateTime: 2023-05-23  18:47
 * @Description: generator for insert sql
 * @Version: 1.0
 */
public class InsertReverser extends SqlReverser {
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
        StringBuilder sb = new StringBuilder();
        String tableName = getTableName(sql);
        List<String> primaryKeys = MetaData.getPrimaryKey(tableName);
        if (primaryKeys == null || primaryKeys.size() == 0) {
            end();
            throw new RuntimeException("Can't find primary key in table "+tableName);
        }
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            sb.append(DELETE).append(" ").append(FROM).append(" ").append(tableName).append(" ").append(WHERE).append(" ");
            sb.append(primaryKeys.stream().map(key -> key + " = ?").collect(Collectors.joining(" AND ")));
            rollBackSql.setSql(sb.toString());
            Map<String, String> primaryValues = getPrimaryValues(sql, primaryKeys);
            List<Object> param = new ArrayList<>();
            DatabaseMetaData metaData = conn.getMetaData();
            for (String key : primaryValues.keySet()) {
                ResultSet columns = metaData.getColumns(null, null, tableName, key);
                if (!columns.next()) {
                    throw new RuntimeException("Can not find column " + key + " in table " + tableName);
                }
                int dataType = columns.getInt("DATA_TYPE");
                if (dataType == Types.INTEGER || dataType == Types.BIGINT || dataType == Types.SMALLINT || dataType == Types.TINYINT) {
                    param.add(Integer.parseInt(primaryValues.get(key)));
                } else if (dataType == Types.DOUBLE || dataType == Types.FLOAT || dataType == Types.DECIMAL) {
                    param.add(Double.parseDouble(primaryValues.get(key)));
                } else if (dataType == Types.DATE || dataType == Types.TIME || dataType == Types.TIMESTAMP) {
                    param.add(Date.valueOf(primaryValues.get(key)));
                } else if (dataType == Types.BOOLEAN) {
                    param.add(Boolean.parseBoolean(primaryValues.get(key)));
                } else if (dataType == Types.CHAR || dataType == Types.VARCHAR || dataType == Types.LONGVARCHAR) {
                    param.add(primaryValues.get(key));
                } else {
                    param.add(primaryValues.get(key));
                }
            }
            params.add(param);
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
        int index = Math.max(sql.indexOf(INTO), sql.indexOf(INTO.toUpperCase()));
        int start = index + INTO.length();
        String[] wholeName = skipSpace(sql, start).replaceAll("`", "").split("\\.");
        if (wholeName.length > 1) {
            return wholeName[wholeName.length-1];
        }
        return wholeName[0];
    }

    /**
     * get primaryKey's value , multiple if multi primaries r defined
     *
     * @param sql sql
     * @return {@link Map}<{@link String}, {@link String}>
     */
    @Override
    Map<String, String> getPrimaryValues(String sql, List<String> primaryKeys) {
        Map<String, String> map = new HashMap<>(1);
        for (String primaryKey : primaryKeys) {
            int t = sql.contains(primaryKey.toUpperCase()) ? sql.indexOf(primaryKey.toUpperCase()) : sql.indexOf(primaryKey.toLowerCase());
            int pos = 0;
            while (t > 0 && sql.charAt(t) != '(') {
                if (sql.charAt(t) == ',') {
                    pos++;
                }
                t--;
            }
            int l = sql.lastIndexOf('(');
            int r = 0;
            while (l < sql.length() && sql.charAt(l) != ')') {
                if (pos == 0) {
                    int k = l;
                    while (k < sql.length()) {
                        if (sql.charAt(k) != ',' && sql.charAt(k) != ')' && sql.charAt(k) != ' ') {
                            break;
                        }
                        k++;
                    }
                    while (k < sql.length() && sql.charAt(k) != ',' && sql.charAt(k) != ')' && sql.charAt(k) != ' ') {
                        k++;
                    }
                    r = k;
                    break;
                }
                if (sql.charAt(l) == ',') {
                    pos--;
                }
                l++;
            }
            map.put(primaryKey, sql.substring(l + 1, r));
        }
        return map;
    }
}
