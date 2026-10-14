package com.ingot.cloud.iam.persistence;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * <p>记录实际 JDBC 查询执行，用于验证分页行数增加不会线性增加 SQL。</p>
 * @author jy
 * @since 1.0.0
 */
public final class QueryCountingDataSource extends DelegatingDataSource {
    private final List<String> queries = new ArrayList<>();

    /** 包装隔离测试数据源，不改变查询或事务语义。 */
    public QueryCountingDataSource(DataSource source) { super(source); }

    /** 清除准备数据或上一轮查询记录。 */
    public void reset() { queries.clear(); }

    /** 返回真实执行的 SELECT SQL 数量。 */
    public int count() { return queries.size(); }

    /** 返回直接读取指定表的 SQL 数量，包含分页 COUNT。 */
    public long from(String table) { return queries.stream().filter(sql -> sql.contains("from " + table)).count(); }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection() throws SQLException { return wrap(super.getConnection()); }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return wrap(super.getConnection(username, password));
    }

    private Connection wrap(Connection connection) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    try {
                        Object value = method.invoke(connection, args);
                        if (method.getName().equals("prepareStatement") && value instanceof PreparedStatement statement) {
                            String sql = ((String) args[0]).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
                            return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                    new Class<?>[]{PreparedStatement.class}, (statementProxy, operation, parameters) -> {
                                        if (sql.stripLeading().startsWith("select") && (operation.getName().equals("executeQuery")
                                                || operation.getName().equals("execute"))) queries.add(sql);
                                        try { return operation.invoke(statement, parameters); }
                                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                                    });
                        }
                        return value;
                    } catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}
