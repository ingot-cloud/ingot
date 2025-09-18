package com.ingot.cloud.pms.api.model.dto.user;

import java.io.Serializable;
import java.util.List;

import com.ingot.framework.commons.model.common.AllowTenantDTO;
import lombok.Data;

/**
 * <p>Description  : UserInfoDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/6.</p>
 * <p>Time         : 11:36 上午.</p>
 */
@Data
public class UserInfoDTO implements Serializable {
    /**
     * 用户信息
     */
    private UserBaseInfoDTO user;
    /**
     * 拥有角色
     */
    private List<String> roles;
    /**
     * 可以访问的租户
     */
    private List<AllowTenantDTO> allows;
}
