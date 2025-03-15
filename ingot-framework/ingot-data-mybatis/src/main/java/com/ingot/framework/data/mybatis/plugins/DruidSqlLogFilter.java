package com.ingot.framework.data.mybatis.plugins;

import com.alibaba.druid.DbType;
import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.filter.FilterEventAdapter;
import com.alibaba.druid.proxy.jdbc.JdbcParameter;
import com.alibaba.druid.proxy.jdbc.ResultSetProxy;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.util.StringUtils;
import com.ingot.framework.data.mybatis.properties.MybatisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Description  : DruidSqlLogFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/10.</p>
 * <p>Time         : 1:31 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DruidSqlLogFilter extends FilterEventAdapter {

    private static final SQLUtils.FormatOption FORMAT_OPTION = new SQLUtils.FormatOption(false, false);

    private final MybatisProperties properties;

    @Override
    protected void statementExecuteBefore(StatementProxy statement, String sql) {
        statement.setLastExecuteStartNano();
    }

    @Override
    protected void statementExecuteBatchBefore(StatementProxy statement) {
        statement.setLastExecuteStartNano();
    }

    @Override
    protected void statementExecuteUpdateBefore(StatementProxy statement, String sql) {
        statement.setLastExecuteStartNano();
    }

    @Override
    protected void statementExecuteQueryBefore(StatementProxy statement, String sql) {
        statement.setLastExecuteStartNano();
    }

    @Override
    protected void statementExecuteAfter(StatementProxy statement, String sql, boolean firstResult) {
        statement.setLastExecuteTimeNano();
    }

    @Override
    protected void statementExecuteBatchAfter(StatementProxy statement, int[] result) {
        statement.setLastExecuteTimeNano();
    }

    @Override
    protected void statementExecuteQueryAfter(StatementProxy statement, String sql, ResultSetProxy resultSet) {
        statement.setLastExecuteTimeNano();
    }

    @Override
    protected void statementExecuteUpdateAfter(StatementProxy statement, String sql, int updateCount) {
        statement.setLastExecuteTimeNano();
    }

    @Override
    public void statement_close(FilterChain chain, StatementProxy statement) throws SQLException {
        // 先调用父类关闭 statement
        super.statement_close(chain, statement);
        // 支持动态开启
        if (!properties.isShowSqlLog()) {
            return;
        }

        // 是否开启调试
        if (!log.isInfoEnabled()) {
            return;
        }

        // 打印可执行的 sql
        String sql = statement.getBatchSql();
        // sql 为空直接返回
        if (StringUtils.isEmpty(sql)) {
            return;
        }

        String dbType = statement.getConnectionProxy().getDirectDataSource().getDbType();

        int parametersSize = statement.getParametersSize();
        List<Object> parameters = new ArrayList<>(parametersSize);
        for (int i = 0; i < parametersSize; ++i) {
            // 转换参数，处理 java8 时间
            parameters.add(getJdbcParameter(statement.getParameter(i)));
        }
        String formattedSql = SQLUtils.format(sql, DbType.of(dbType), parameters, FORMAT_OPTION);
        printSql(formattedSql, statement);
    }

    private static Object getJdbcParameter(JdbcParameter jdbcParam) {
        if (jdbcParam == null) {
            return null;
        }
        Object value = jdbcParam.getValue();
        // 处理 java8 时间
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        return value;
    }

    private static void printSql(String sql, StatementProxy statement) {
        // 打印 sql
        String sqlLogger = """
                
                
                ========= Sql Logger =========
                {}
                ========= Sql Execute Time: {}
                """;
        log.info(sqlLogger, sql.trim(), format(statement.getLastExecuteTimeNano()));
    }

    /**
     * 格式化执行时间，单位为 ms 和 s，保留三位小数
     * @param nanos 纳秒
     * @return 格式化后的时间
     */
    private static String format(long nanos) {
        if (nanos < 1) {
            return "0ms";
        }
        double millis = (double) nanos / (1000 * 1000);
        // 不够 1 ms，最小单位为 ms
        if (millis > 1000) {
            return String.format("%.3fs", millis / 1000);
        }
        else {
            return String.format("%.3fms", millis);
        }
    }

    /**
     * 从SQL中提取表名(sql中出现的所有表)
     * @param sql sql语句
     * @param dbType dbType
     * @return List<String>
     */
    public static List<String> getTablesBydruid(String sql, String dbType) {
        List<String> result = new ArrayList<String>();
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, dbType);
        for (SQLStatement stmt : stmtList) {
            // 也可以用更精确的解析器，如MySqlSchemaStatVisitor
            SchemaStatVisitor visitor = new SchemaStatVisitor();
            stmt.accept(visitor);
            Map<TableStat.Name, TableStat> tables = visitor.getTables();
            for (TableStat.Name name : tables.keySet()) {
                result.add(name.getName());
            }
        }
        return result;
    }
}
