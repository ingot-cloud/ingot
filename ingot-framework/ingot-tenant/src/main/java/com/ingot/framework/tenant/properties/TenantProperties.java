package com.ingot.framework.tenant.properties;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : TenantProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/18.</p>
 * <p>Time         : 2:55 下午.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.tenant")
public class TenantProperties {

    /**
     * 租户字段名
     */
    private String column = "tenant_id";

    /**
     * 需要进行数据隔离的表
     */
    private List<String> tables = new ArrayList<>();
}
