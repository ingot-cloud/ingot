package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.core.model.common.AllowTenantDTO;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * <p>Description  : IngotUserDetails.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/4.</p>
 * <p>Time         : 11:09 AM.</p>
 */
public interface IngotUserDetails extends UserDetails {

    /**
     * 返回允许访问的组织租户信息
     *
     * @return {@link AllowTenantDTO}
     */
    List<AllowTenantDTO> getAllows();
}
