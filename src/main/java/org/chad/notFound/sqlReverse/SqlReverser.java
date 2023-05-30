package org.chad.notFound.sqlReverse;

import org.chad.notFound.configuration.FistProperties;
import org.chad.notFound.configuration.FistSpringContextHolder;
import org.chad.notFound.model.RollBackSql;
import org.chad.notFound.threadLocal.FistThreadLocal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.sqlRevert
 * @Author: hyl
 * @CreateTime: 2023-05-23  17:43
 * @Description: to adapt update insert delete sql to generate reversed sql
 * @Version: 1.0
 */
public abstract class SqlReverser {
    protected static final String URL;
    protected static final String USERNAME;
    protected static final String PASSWORD;

    static {
        FistProperties fistProperties = FistSpringContextHolder.getBean(FistProperties.class);
        URL = fistProperties.getFistTargetDatabaseUrl();
        USERNAME = fistProperties.getFistTargetDatabaseUsername();
        PASSWORD = fistProperties.getFistTargetDatabasePassword();
    }



    /**
     * generate rollback sql
     *
     * @param sql sql
     * @return {@link String}
     */
    abstract public RollBackSql generate(String sql);

    /**
     * get table name of this sql
     *
     * @param sql sql
     * @return {@link String}
     */
    abstract String getTableName(String sql);

    /**
     * get primaryKey's value , multiple if multi primaries r defined
     *
     * @param sql sql
     * @return {@link Map}<{@link String}, {@link String}>
     */
    Map<String, String> getPrimaryValues(String sql, List<String> primaryKeys) {
        return null;
    }

    /**
     * sql generate starts
     */
    protected String start(String sql) {
        FistThreadLocal.CLOSEABLE.set("CLOSEABLE");
        return sql.trim().replaceAll("\n", " ").replaceAll("\t", " ");
    }

    /**
     * sql generate ends
     */
    protected void end() {
        FistThreadLocal.CLOSEABLE.remove();
    }

    /**
     * skip space to get readable context
     *
     * @param sql   sql
     * @param start start
     * @return {@link String}
     */
    protected String skipSpace(String sql, int start) {
        while (start < sql.length() && (sql.charAt(start) == ' ' || sql.charAt(start) == '(' || sql.charAt(start) == ')')) {
            start++;
        }
        int l = start + 1;
        while (l < sql.length() && sql.charAt(l) != ' ' && sql.charAt(l) != '(' && sql.charAt(start) != ')') {
            l++;
        }
        return sql.substring(start, l);
    }

    /**
     * predicate data type to predicate if needs to add ''
     *
     * @param rs              resultSet
     * @param columnDataTypes columnDataTypes
     * @param param           param
     * @param key             key
     * @throws SQLException sqlexception
     */
    protected void predicateDataType(ResultSet rs, Map<String, Integer> columnDataTypes, List<Object> param, String key) throws SQLException {
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
}
