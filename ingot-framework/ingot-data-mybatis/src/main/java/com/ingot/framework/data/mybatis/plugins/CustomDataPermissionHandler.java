package com.ingot.framework.data.mybatis.plugins;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.ingot.framework.data.mybatis.scope.config.DataScopeProperties;
import com.ingot.framework.data.mybatis.scope.context.DataScopeContextHolder;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.List;

/**
 * <p>Description  : CustomDataPermissionHandler.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/1.</p>
 * <p>Time         : 16:50.</p>
 */
@RequiredArgsConstructor
public class CustomDataPermissionHandler implements MultiDataPermissionHandler {
    private final DataScopeProperties properties;
    private static final Expression EMPTY_SCOPE = new EqualsTo(new LongValue(1), new LongValue(2));

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        List<String> tables = properties.getTables();
        String name = table.getName();
        if (CollUtil.isEmpty(tables) || !tables.contains(name)) {
            return null;
        }

        // 跳过直接返回
        if (DataScopeContextHolder.isSkip()) {
            return null;
        }

        // 如果没有权限直接追加 1=2
        if (DataScopeContextHolder.isEmpty()) {
            return EMPTY_SCOPE;
        }

        List<Long> scopes = DataScopeContextHolder.getScopes();
        Long userScope = DataScopeContextHolder.getUserScope();
        if (CollUtil.isNotEmpty(scopes) && userScope != null) {
            return AndExpression.add(
                    new EqualsTo(getAliasColumn(table, properties.getUserFieldName()),
                            new LongValue(userScope)),
                    new InExpression(getAliasColumn(table, properties.getScopeFieldName()),
                            new ExpressionList<>(scopes.stream().map(LongValue::new).toList()))
            );
        }

        if (userScope != null) {
            return new EqualsTo(getAliasColumn(table, properties.getUserFieldName()),
                    new LongValue(userScope));
        }

        return new InExpression(getAliasColumn(table, properties.getScopeFieldName()),
                new ExpressionList<>(scopes.stream().map(LongValue::new).toList()));
    }

    protected Column getAliasColumn(Table table, String field) {
        StringBuilder column = new StringBuilder();
        if (table.getAlias() != null) {
            column.append(table.getAlias().getName()).append(StringPool.DOT);
        }
        column.append(field);
        return new Column(column.toString());
    }
}
