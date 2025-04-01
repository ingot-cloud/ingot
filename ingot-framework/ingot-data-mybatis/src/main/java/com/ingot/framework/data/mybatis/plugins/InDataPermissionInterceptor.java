package com.ingot.framework.data.mybatis.plugins;

import com.baomidou.mybatisplus.extension.plugins.inner.BaseMultiTableInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;

/**
 * <p>Description  : InDataPermissionInterceptor.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/31.</p>
 * <p>Time         : 15:02.</p>
 */
@Slf4j
public class InDataPermissionInterceptor extends BaseMultiTableInnerInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor,
                            MappedStatement ms,
                            Object parameter,
                            RowBounds rowBounds,
                            ResultHandler resultHandler,
                            BoundSql boundSql) throws SQLException {

    }

    @Override
    public Expression buildTableExpression(Table table, Expression where, String whereSegment) {
        return where;
    }

}
