package com.ingot.framework.data.mybatis.plugins;

import java.util.List;

import com.ingot.framework.data.mybatis.scope.config.DataScopeProperties;
import com.ingot.framework.data.mybatis.scope.context.DataScopeContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>本人与部门范围必须用 OR 合并，跨资源表不得套用当前帧。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CustomDataPermissionHandlerTest {

    @AfterEach
    void tearDown() {
        DataScopeContextHolder.clear();
    }

    @Test
    void selfAndDeptUseOr() {
        DataScopeProperties properties = new DataScopeProperties();
        properties.getTables().add("t_order");
        CustomDataPermissionHandler handler = new CustomDataPermissionHandler(properties);
        DataScopeContextHolder.push(DataScopeContextHolder.Frame.of(
                "order", "demo:order:query", false, List.of(3L), 10L));
        Expression expression = handler.getSqlSegment(new Table("t_order"), null, "id");
        assertTrue(expression instanceof ParenthesedExpressionList);
        assertTrue(expression.toString().contains("OR"));
        assertTrue(((ParenthesedExpressionList<?>) expression).get(0) instanceof OrExpression);
    }

    @Test
    void skipAllAddsNothing() {
        DataScopeProperties properties = new DataScopeProperties();
        properties.getTables().add("t_order");
        CustomDataPermissionHandler handler = new CustomDataPermissionHandler(properties);
        DataScopeContextHolder.push(DataScopeContextHolder.Frame.of(
                "order", "demo:order:query", true, List.of(), null));
        assertNull(handler.getSqlSegment(new Table("t_order"), null, "id"));
    }

    @Test
    void emptyFrameIsClosed() {
        DataScopeProperties properties = new DataScopeProperties();
        properties.getTables().add("t_order");
        CustomDataPermissionHandler handler = new CustomDataPermissionHandler(properties);
        DataScopeContextHolder.push(DataScopeContextHolder.Frame.of(
                "order", "demo:order:query", false, List.of(), null));
        Expression expression = handler.getSqlSegment(new Table("t_order"), null, "id");
        assertNotNull(expression);
        assertTrue(expression.toString().contains("1 = 2") || expression.toString().contains("1=2"));
    }

    @Test
    void mismatchedResourceIsClosed() {
        DataScopeProperties properties = new DataScopeProperties();
        properties.getTables().add("t_notice");
        DataScopeProperties.TableMapping mapping = new DataScopeProperties.TableMapping();
        mapping.setTable("t_notice");
        mapping.setResource("notice");
        properties.getTableMappings().add(mapping);
        CustomDataPermissionHandler handler = new CustomDataPermissionHandler(properties);
        DataScopeContextHolder.push(DataScopeContextHolder.Frame.of(
                "order", "demo:order:query", true, List.of(), null));
        Expression expression = handler.getSqlSegment(new Table("t_notice"), null, "id");
        assertNotNull(expression);
        assertTrue(expression.toString().contains("1 = 2") || expression.toString().contains("1=2"));
    }
}
