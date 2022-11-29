package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.store.mybatis.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_oauth_client")
public class SysRoleOauthClient extends BaseModel<SysRoleAuthority> {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 客户端ID
     */
    private String clientId;

}
