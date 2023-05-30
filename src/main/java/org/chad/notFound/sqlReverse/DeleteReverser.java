package org.chad.notFound.sqlReverse;

import org.chad.notFound.model.RollBackSql;

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
 * @Description: generator for delete sql
 * @Version: 1.0
 */
public class DeleteReverser extends SqlReverser {
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
        int indexOfWhere = Math.max(sql.indexOf(WHERE), sql.indexOf(WHERE.toUpperCase()));
        String whereCause = sql.substring(indexOfWhere);
        String selectSql = String.format("select * from %s %s", tableName, whereCause);
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             PreparedStatement pst = conn.prepareStatement(selectSql);
             ResultSet rs = pst.executeQuery()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(rsmd.getColumnName(i));
            }
            DatabaseMetaData metaData = conn.getMetaData();
            Map<String, Integer> columnDataTypes = new HashMap<>();
            for (String column : columns) {
                ResultSet columnsMetaData = metaData.getColumns(null, null, tableName, column);
                if (!columnsMetaData.next()) {
                    throw new RuntimeException("Can't find column " + column + " in table " + tableName);
                }
                int dataType = columnsMetaData.getInt("DATA_TYPE");
                columnDataTypes.put(column, dataType);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(INSERT).append(" ").append(INTO).append(" ").append(tableName).append(" ").append("(").append(String.join(",", columns)).append(")").append(" ").append(VALUES).append(" ").append("(").append(columns.stream().map(column -> "?").collect(Collectors.joining(","))).append(")");
            rollBackSql.setSql(sb.toString());
            while (rs.next()) {
                List<Object> param = new ArrayList<>();
                for (String column : columns) {
                    predicateDataType(rs, columnDataTypes, param, column);
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
        int index = Math.max(sql.indexOf(FROM), sql.indexOf(FROM.toUpperCase()));
        int start = index + FROM.length();
        String[] wholeName = skipSpace(sql, start).replaceAll("`", "").split("\\.");
        if (wholeName.length > 1) {
            return wholeName[wholeName.length - 1];
        }
        return wholeName[0];
    }
}
