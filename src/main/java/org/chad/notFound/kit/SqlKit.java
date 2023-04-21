package org.chad.notFound.kit;

import org.chad.notFound.configuration.FistProperties;
import org.chad.notFound.configuration.FistSpringContextHolder;
import org.chad.notFound.model.RollBackSql;
import org.chad.notFound.model.db.MetaData;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.kit
 * @Author: hyl
 * @CreateTime: 2023-04-04  14:58
 * @Description: kit to generate rollback sql base on sql and metadata
 * This kit may not in the best performance, I'll reconsider it later.
 * Due to supporting multiple databases,I can't just load column data type from Application runner,
 * so I need to use prepared statement to use jdbc auto convert data type.
 * But I don't want to remain any stupid thing in java client,so I need to prepare sql and fulfill the value and then
 * send to rust client.
 * @Version: 1.0
 */
public class SqlKit {
    private static final String INSERT = "insert";
    private static final String UPDATE = "update";
    private static final String DELETE = "delete";
    private static final String FROM = "from";
    private static final String WHERE = "where";
    private static final String SET = "set";
    private static final String VALUES = "values";
    private static final String INTO = "into";
    private static final String AND = "and";
    private static final String OR = "or";
    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;

    static {
        FistProperties fistProperties = FistSpringContextHolder.getBean(FistProperties.class);
        URL = fistProperties.getFistTargetDatabaseUrl();
        USERNAME = fistProperties.getFistTargetDatabaseUsername();
        PASSWORD = fistProperties.getFistTargetDatabasePassword();
    }

    public static final ThreadLocal<String> CLOSEABLE = new ThreadLocal<>();

    /**
     * To generate rollback sql for update
     *
     * @param sql sql
     * @return {@link RollBackSql}
     */
    public static RollBackSql rollbackUpdate(String sql) {
        CLOSEABLE.set("closeable");
        RollBackSql rollBackSql = new RollBackSql();
        List<List<Object>> params = new ArrayList<>();
        rollBackSql.setParams(params);
        String tableName = getUpdateSqlTableName(sql);
        List<String> primaryKey = MetaData.getPrimaryKey(tableName);
        if (primaryKey == null || primaryKey.isEmpty()) {
            throw new RuntimeException("Fist supports table with primary key only for now !");
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
            e.printStackTrace();
        } finally {
            CLOSEABLE.remove();
        }
        return rollBackSql;
    }

    /**
     * predicate data type to predicate if needs to add ''
     *
     * @param rs              resultSet
     * @param columnDataTypes columnDataTypes
     * @param param           param
     * @param key             key
     * @throws SQLException sqlexception异常
     */
    private static void predicateDataType(ResultSet rs, Map<String, Integer> columnDataTypes, List<Object> param, String key) throws SQLException {
        int dataType = columnDataTypes.get(key);
        if (dataType == Types.BIGINT || dataType == Types.INTEGER || dataType == Types.SMALLINT || dataType == Types.TINYINT) {
            param.add(rs.getLong(key));
        } else if (dataType == Types.DECIMAL || dataType == Types.DOUBLE || dataType == Types.FLOAT || dataType == Types.NUMERIC || dataType == Types.REAL) {
            param.add(rs.getDouble(key));
        } else if (dataType == Types.DATE || dataType == Types.TIME || dataType == Types.TIMESTAMP) {
            param.add(rs.getDate(key));
        } else if (dataType == Types.CHAR || dataType == Types.VARCHAR || dataType == Types.LONGVARCHAR) {
            param.add(rs.getString(key));
        } else if (dataType == Types.BINARY || dataType == Types.VARBINARY || dataType == Types.LONGVARBINARY) {
            param.add(rs.getBytes(key));
        } else if (dataType == Types.BIT || dataType == Types.BOOLEAN) {
            param.add(rs.getBoolean(key));
        } else if (dataType == Types.CLOB) {
            param.add(rs.getClob(key));
        } else if (dataType == Types.BLOB) {
            param.add(rs.getBlob(key));
        } else if (dataType == Types.ARRAY) {
            param.add(rs.getArray(key));
        } else if (dataType == Types.REF) {
            param.add(rs.getRef(key));
        } else if (dataType == Types.STRUCT) {
            param.add(rs.getObject(key));
        } else if (dataType == Types.DATALINK) {
            param.add(rs.getURL(key));
        } else if (dataType == Types.JAVA_OBJECT) {
            param.add(rs.getObject(key));
        } else if (dataType == Types.NCHAR || dataType == Types.NVARCHAR || dataType == Types.LONGNVARCHAR) {
            param.add(rs.getNString(key));
        } else if (dataType == Types.NCLOB) {
            param.add(rs.getNClob(key));
        } else if (dataType == Types.SQLXML) {
            param.add(rs.getSQLXML(key));
        } else if (dataType == Types.ROWID) {
            param.add(rs.getRowId(key));
        } else if (dataType == Types.DISTINCT) {
            param.add(rs.getObject(key));
        } else if (dataType == Types.OTHER) {
            param.add(rs.getObject(key));
        } else if (dataType == Types.NULL) {
            param.add(rs.getObject(key));
        } else if (dataType == Types.REF_CURSOR) {
            param.add(rs.getObject(key));
        } else {
            param.add(rs.getObject(key));
        }
    }


    /**
     * To generate rollback sql for insert
     *
     * @param sql sql
     * @return {@link RollBackSql}
     */
    public static RollBackSql rollbackInsert(String sql) {
        CLOSEABLE.set("CLOSEABLE");
        RollBackSql rollBackSql = new RollBackSql();
        List<List<Object>> params = new ArrayList<>();
        rollBackSql.setParams(params);
        StringBuilder sb = new StringBuilder();
        String tableName = getInsertSqlTableName(sql);
        List<String> primaryKeys = MetaData.getPrimaryKey(tableName);
        if (primaryKeys == null || primaryKeys.size() == 0) {
            throw new RuntimeException("Fist supports table with primary key only for now !");
        }
        try {
            sb.append(DELETE).append(" ").append(FROM).append(" ").append(tableName).append(" ").append(WHERE).append(" ");
            sb.append(primaryKeys.stream().map(key -> key + " = ?").collect(Collectors.joining(" AND ")));
            rollBackSql.setSql(sb.toString());
            Map<String, String> primaryValues = getPrimaryKeyValues(sql, primaryKeys);
            List<Object> param = new ArrayList<>();
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
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
            CLOSEABLE.remove();
        }
        return rollBackSql;
    }

    /**
     * To generate rollback sql for delete,which needs to select first.
     *
     * @param sql sql
     * @return {@link RollBackSql}
     */
    public static RollBackSql rollbackDelete(String sql) {
        CLOSEABLE.set("CLOSEABLE");
        RollBackSql rollBackSql = new RollBackSql();
        List<List<Object>> params = new ArrayList<>();
        rollBackSql.setParams(params);
        String tableName = getDeleteSqlTableName(sql);
        StringBuilder selectSql = new StringBuilder();
        int whereIndex = Math.max(sql.indexOf(WHERE), sql.indexOf(WHERE.toUpperCase()));
        selectSql.append("select * from ").append(tableName).append(" ").append(sql.substring(whereIndex));

        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD); PreparedStatement ps = conn.prepareStatement(selectSql.toString()); ResultSet rs = ps.executeQuery()) {

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
            e.printStackTrace();
        } finally {
            CLOSEABLE.remove();
        }
        return rollBackSql;
    }

    /**
     * get table name from a update sql
     *
     * @param sql sql
     * @return {@link String}
     */
    private static String getUpdateSqlTableName(String sql) {
        int index = Math.max(sql.indexOf(UPDATE), sql.indexOf(UPDATE.toUpperCase()));
        int start = index + UPDATE.length();
        return skipSpace(sql, start).replaceAll("`", "").split("\\.")[0];
    }

    /**
     * get table name from a insert sql
     *
     * @param sql sql
     * @return {@link String}
     */
    private static String getInsertSqlTableName(String sql) {
        int index = Math.max(sql.indexOf(INTO), sql.indexOf(INTO.toUpperCase()));
        int start = index + INTO.length();
        return skipSpace(sql, start).replaceAll("`", "").split("\\.")[0];
    }

    /**
     * get table name from a delete sql
     *
     * @param sql sql
     * @return {@link String}
     */
    private static String getDeleteSqlTableName(String sql) {
        int index = Math.max(sql.indexOf(FROM), sql.indexOf(FROM.toUpperCase()));
        int start = index + FROM.length();
        return skipSpace(sql, start).replaceAll("`", "").split("\\.")[0];
    }

    /**
     * skip space to get read context
     *
     * @param sql   sql
     * @param start 开始
     * @return {@link String}
     */
    private static String skipSpace(String sql, int start) {
        while (start < sql.length() && (sql.charAt(start) == ' ' || sql.charAt(start) == '(' || sql.charAt(start) == ')')) {
            start++;
        }
        int l = start+1;
        while (l < sql.length() && sql.charAt(l) != ' ' && sql.charAt(l) != '(' && sql.charAt(start) != ')') {
            l++;
        }
        return sql.substring(start, l);
    }

    /**
     * get all update values from a update sql
     *
     * @param sql sql
     * @return {@link Map}<{@link String}, {@link String}>
     */
    private static Map<String, String> getAllUpdateValues(String sql) {
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

    /**
     * Through the original sql,find the primary key value
     *
     * @param sql         sql
     * @param primaryKeys primaryKey column name
     * @return {@link List}<{@link String}>
     */
    private static Map<String, String> getPrimaryKeyValues(String sql, List<String> primaryKeys) {
        Map<String, String> map = new HashMap<>();
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
