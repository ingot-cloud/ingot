package com.ingot.framework.data.mybatis.plugins;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

/**
 * <p>Description  : IngotTenantLineHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/18.</p>
 * <p>Time         : 2:50 下午.</p>
 */
@RequiredArgsConstructor
public class IngotTenantLineHandler implements TenantLineHandler {
    private final TenantProperties tenantProperties;

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContextHolder.get();
        if (tenantId == null) {
            return new NullValue();
        }
        return new LongValue(tenantId);
    }

    /**
     * 获取租户字段名
     * 默认字段名叫: tenant_id
     *
     * @return 租户字段名
     */
    @Override
    public String getTenantIdColumn() {
        return tenantProperties.getColumn();
    }

    /**
     * 根据表名判断是否忽略拼接多租户条件
     * <p>
     * 默认都要进行解析并拼接多租户条件
     *
     * @param tableName 表名
     * @return 是否忽略, true:表示忽略，false:需要解析并拼接多租户条件
     */
    @Override
    public boolean ignoreTable(String tableName) {
        Long tenantId = TenantContextHolder.get();
        // 如果当前租户为null那么不进行数据隔离
        if (tenantId == null) {
            return Boolean.TRUE;
        }
        // 如果使用了默认租户，那么不进行数据隔离
        if (TenantContextHolder.isUseDefault()) {
            return Boolean.TRUE;
        }

        return !tenantProperties.getTables().contains(tableName);
    }
}
