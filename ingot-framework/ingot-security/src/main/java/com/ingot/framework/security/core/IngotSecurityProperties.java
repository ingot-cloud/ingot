package com.ingot.framework.security.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : IngotSecurityProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/2/16.</p>
 * <p>Time         : 10:34 AM.</p>
 */
@ConfigurationProperties(prefix = "ingot.security")
public class IngotSecurityProperties {
    /**
     * 忽略租户验证的角色编码列表
     */
    @Getter
    @Setter
    private List<String> ignoreTenantValidateRoleCodeList = new ArrayList<>();
}
