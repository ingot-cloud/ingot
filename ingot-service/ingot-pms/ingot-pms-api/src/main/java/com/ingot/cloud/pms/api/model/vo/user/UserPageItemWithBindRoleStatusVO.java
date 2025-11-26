package com.ingot.cloud.pms.api.model.vo.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserPageItemWithBindRoleStatusVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/26.</p>
 * <p>Time         : 15:23.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserPageItemWithBindRoleStatusVO extends UserPageItemVO {
    /**
     * 当前用户是否可以绑定角色
     */
    private boolean canBind;
}
