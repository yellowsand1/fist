package org.chad.notFound.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.chad.notFound.kit.SqlKit;
import org.chad.notFound.model.Sql;
import org.chad.notFound.service.impl.SagaCallbackServiceImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * sql record,clear every request
 *
 * @author hyl
 * @date 2023/04/06
 */
@Slf4j
@Aspect
public class JdbcConnectionAspect {
    /**
     * This map is used to record the sql of the current connection,
     * may have multiple connections in one request,
     * but different method use different connection,
     * when to aop the method that has GlobalTransactional annotation,
     * I can get the current connection that the method using,
     * so that I can choose which sql needs to be recorded.
     * I can't remove after every aop, because I may support propagation in the future.
     */
    public static final ThreadLocal<List<Sql>> SQL_LIST = new ThreadLocal<>();
    public static final ThreadLocal<List<Connection>> CONNECTIONS = new InheritableThreadLocal<>();

    @Around("execution(* javax.sql.DataSource.getConnection(..))")
    public Object interceptGetConnection(ProceedingJoinPoint joinPoint) throws Throwable {
        // original connection
        Connection connection = (Connection) joinPoint.proceed();

        if (SagaCallbackServiceImpl.ROLLBACK.get() != null || SqlKit.CLOSEABLE.get() != null) {
            return connection;
        }
        // proxy of connection
        // due to the proxy,the classloader of the proxy is different from the original connection
        // if the original connection take's different classloader, but I won't consider it now.
        if (GlobalTransactionAspect.CLOSEABLE.get() != null) {
            Object res = Proxy.newProxyInstance(JdbcConnectionAspect.class.getClassLoader(), new Class[]{Connection.class}, new JdbcConnectionHandler(connection));
            List<Connection> connections = CONNECTIONS.get();
            if (connections == null) {
                connections = new ArrayList<>();
            }
            connections.add((Connection) res);
            CONNECTIONS.set(connections);
            ((Connection) res).setAutoCommit(false);
            return res;
        }
        return connection;
    }

    /**
     * this might cause problem,because I can't get the original connection out of the proxy,
     * so I can't get the sql list.
     * This threadLocal map's key is the original connection,but if I put proxy connection to this map,
     * I highly doubt that the proxy connection is the only instance that I can't separate from different
     * connection.
     * Second thought,if there's only one connection in one request and I guarantee that I remove the
     * threadLocal every request,I don't need a map at all,I can't think of a situation that there's
     * a sql needs to be rollback but another sql doesn't except propagation which I'll support and
     * use a different way to implement it.
     *
     * @param sql sql
     */
    public static void handleSql(String sql) {
        if (isUpdateSql(sql)) {
            List<Sql> sqls = SQL_LIST.get();
            if (sqls == null) {
                sqls = new ArrayList<>();
            }
            sqls.add(new Sql(sql));
            SQL_LIST.set(sqls);
        }
    }

    /**
     * predicate if the sql might change the data
     *
     * @param sql sql
     * @return boolean
     */
    private static boolean isUpdateSql(String sql) {
        String upperCaseSql = sql.trim().toUpperCase();
        return upperCaseSql.startsWith("INSERT") || upperCaseSql.startsWith("UPDATE") || upperCaseSql.startsWith("DELETE");
    }

    private static class JdbcConnectionHandler implements InvocationHandler {

        private Connection connection;

        public JdbcConnectionHandler(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("prepareStatement".equals(methodName)) {
                PreparedStatement preparedStatement = (PreparedStatement) method.invoke(connection, args);
                String sql = (String) args[0];
                if (!sql.contains("?")) {
                    handleSql(sql);
                    return preparedStatement;
                }
                if (SagaCallbackServiceImpl.ROLLBACK.get() != null || SqlKit.CLOSEABLE.get() != null) {
                    return preparedStatement;
                }
                return Proxy.newProxyInstance(JdbcPreparedStatementHandler.class.getClassLoader(), new Class[]{PreparedStatement.class}, new JdbcPreparedStatementHandler(preparedStatement, sql));
            } else if ("createStatement".equals(methodName)) {
                Statement statement = (Statement) method.invoke(connection, args);
                if (SagaCallbackServiceImpl.ROLLBACK.get() != null || SqlKit.CLOSEABLE.get() != null) {
                    return statement;
                }
                return Proxy.newProxyInstance(JdbcConnectionHandler.class.getClassLoader(), new Class[]{Statement.class}, new JdbcStatementHandler(statement));
            } else if ("close".equals(methodName)) {
                if (GlobalTransactionAspect.CLOSEABLE.get() != null) {
                    return null;
                }
            } else if ("commit".equals(methodName)) {
                if (GlobalTransactionAspect.CLOSEABLE.get() != null) {
                    return null;
                }
            } else if ("rollback".equals(methodName)) {
                if (GlobalTransactionAspect.CLOSEABLE.get() != null) {
                    return null;
                }
            }
            return method.invoke(connection, args);
        }

        private static class JdbcStatementHandler implements InvocationHandler {

            private Statement statement;

            public JdbcStatementHandler(Statement statement) {
                this.statement = statement;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if ("execute".equals(methodName) || "executeUpdate".equals(methodName) || "executeQuery".equals(methodName)) {
                    if (args != null && args.length > 0 && args[0] instanceof String) {
                        String sql = (String) args[0];
                        handleSql(sql);
                    }
                }
                return method.invoke(statement, args);
            }
        }
    }

    private static class JdbcPreparedStatementHandler implements InvocationHandler {

        private PreparedStatement preparedStatement;
        private String sql;
        private Map<Integer, Object> parameters = new HashMap<>();

        public JdbcPreparedStatementHandler(PreparedStatement preparedStatement, String sql) {
            this.preparedStatement = preparedStatement;
            this.sql = sql;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.startsWith("set") && args.length == 2 && args[0] instanceof Integer) {
                int parameterIndex = (Integer) args[0];
                Object value = args[1];
                parameters.put(parameterIndex, value);
            } else if ("execute".equals(methodName) || "executeUpdate".equals(methodName) || "executeQuery".equals(methodName)) {
                String filledSql = fillParameters(sql, parameters);
                handleSql(filledSql);
            }

            return method.invoke(preparedStatement, args);
        }

        private String fillParameters(String sql, Map<Integer, Object> parameters) {
            //may not very critical,maybe I can use a better way to do this when exception occurs
            StringBuilder filledSql = new StringBuilder();
            int lastIndex = 0;
            int index = 1;
            for (int i = 0; i < sql.length(); i++) {
                if (sql.charAt(i) == '?') {
                    filledSql.append(sql, lastIndex, i);
                    filledSql.append(parameters.get(index));
                    lastIndex = i + 1;
                    index++;
                }
            }
            filledSql.append(sql, lastIndex, sql.length());
            return filledSql.toString();
        }
    }
}