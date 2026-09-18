package com.ingot.framework.tenant.properties;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>租户数据隔离相关配置，绑定 {@code ingot.tenant}。</p>
 *
 * <p>仅描述 MyBatis 租户列名和需要隔离的表；请求未携带租户时上下文保持为空，不再提供默认租户 ID。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.tenant")
public class TenantProperties {
    /**
     * 租户字段名，默认 {@code tenant_id}。
     */
    private String column = "tenant_id";

    /**
     * 需要进行数据隔离的表名列表；未列入的表不拼接租户条件。
     */
    private List<String> tables = new ArrayList<>();
}
