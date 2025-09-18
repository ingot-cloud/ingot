package com.ingot.framework.security.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : 安全配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/2/16.</p>
 * <p>Time         : 10:34 AM.</p>
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "ingot.security")
public class InSecurityProperties {
    /**
     * 忽略租户验证的角色编码列表
     */
    private List<String> ignoreTenantValidateRoleCodeList = new ArrayList<>();
}
