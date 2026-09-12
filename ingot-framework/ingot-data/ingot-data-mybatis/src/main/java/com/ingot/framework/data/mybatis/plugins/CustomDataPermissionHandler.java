package com.ingot.framework.data.mybatis.plugins;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.ingot.framework.data.mybatis.scope.config.DataScopeProperties;
import com.ingot.framework.data.mybatis.scope.context.DataScopeContextHolder;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

/**
 * <p>按当前数据范围帧生成行过滤条件：租户条件由租户插件处理，本处理器追加本人或部门谓词。</p>
 *
 * <p>ALL 不加行条件；SELF 为归属用户等值；部门为 {@code IN} 已展开 ID；
 * 二者并集为 {@code OR}；无规则或跨资源套用当前帧时 {@code 1=2}。禁止在内存过滤结果集。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote 受保护表应具备 {@code (tenant_id, 部门列)} 索引；SELF 热路径另加归属用户列索引。
 * 默认 {@code IN} 适合部门几十到几百；组织树过大或 {@code OR} 无法走索引时，见
 * {@code specs/current/pms/data-authorization} 与
 * {@code specs/changes/active/20260912-mybatis-data-scope-predicate-scale}。
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

        DataScopeContextHolder.Frame frame = DataScopeContextHolder.current();
        if (frame == null) {
            return EMPTY_SCOPE;
        }
        DataScopeProperties.TableMapping mapping = properties.findMapping(name);
        if (mapping != null && StrUtil.isNotBlank(mapping.getResource())
                && StrUtil.isNotBlank(frame.resource())
                && !mapping.getResource().equals(frame.resource())) {
            return EMPTY_SCOPE;
        }
        if (DataScopeContextHolder.isSkip()) {
            return null;
        }
        if (DataScopeContextHolder.isEmpty()) {
            return EMPTY_SCOPE;
        }

        String scopeColumn = resolveScopeColumn(mapping);
        String userColumn = resolveUserColumn(mapping);
        List<Long> scopes = DataScopeContextHolder.getScopes();
        Long userScope = DataScopeContextHolder.getUserScope();
        if (CollUtil.isNotEmpty(scopes) && userScope != null) {
            Expression self = new EqualsTo(getAliasColumn(table, userColumn), new LongValue(userScope));
            Expression dept = new InExpression(getAliasColumn(table, scopeColumn),
                    new ExpressionList<>(scopes.stream().map(LongValue::new).toList()));
            return new ParenthesedExpressionList<>(new OrExpression(self, dept));
        }
        if (userScope != null) {
            return new EqualsTo(getAliasColumn(table, userColumn), new LongValue(userScope));
        }
        if (CollUtil.isEmpty(scopes)) {
            return EMPTY_SCOPE;
        }
        return new InExpression(getAliasColumn(table, scopeColumn),
                new ExpressionList<>(scopes.stream().map(LongValue::new).toList()));
    }

    private String resolveScopeColumn(DataScopeProperties.TableMapping mapping) {
        if (mapping != null && StrUtil.isNotBlank(mapping.getScopeColumn())) {
            return mapping.getScopeColumn();
        }
        return properties.getScopeFieldName();
    }

    private String resolveUserColumn(DataScopeProperties.TableMapping mapping) {
        if (mapping != null && StrUtil.isNotBlank(mapping.getUserColumn())) {
            return mapping.getUserColumn();
        }
        return properties.getUserFieldName();
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
