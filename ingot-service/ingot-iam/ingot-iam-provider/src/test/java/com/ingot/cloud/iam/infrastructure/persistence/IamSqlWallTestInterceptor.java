package com.ingot.cloud.iam.infrastructure.persistence;

import java.sql.Connection;

import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.spi.MySqlWallProvider;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

/**
 * <p>在隔离数据库夹具执行最终 SQL 前复用仓库 Wall 配置，覆盖 Wrapper 与分页重写后的语句。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
final class IamSqlWallTestInterceptor implements Interceptor {
    private static final MySqlWallProvider WALL = provider();

    /** 检查最终语句后继续原数据库执行，不改变查询或异常处理。 */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String sql = ((StatementHandler) invocation.getTarget()).getBoundSql().getSql();
        var result = WALL.check(sql);
        if (!result.getViolations().isEmpty()) {
            throw new AssertionError(result.getViolations().stream()
                    .map(v -> v.getErrorCode() + ": " + v.getMessage()).toList() + "\n" + sql);
        }
        return invocation.proceed();
    }

    static MySqlWallProvider provider() {
        var config = new WallConfig();
        // 与 nacos 三个环境的 in-database.yml 保持一致，恒真与恒假检查沿用默认值。
        config.setMultiStatementAllow(true);
        config.setNoneBaseStatementAllow(true);
        config.setCommentAllow(true);
        config.setSelectIntoAllow(false);
        config.setCreateTableAllow(false);
        config.setDropTableAllow(false);
        config.setAlterTableAllow(false);
        config.setTruncateAllow(false);
        return new MySqlWallProvider(config);
    }
}
