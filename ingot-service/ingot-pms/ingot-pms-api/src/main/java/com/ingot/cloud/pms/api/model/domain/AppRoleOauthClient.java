package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2023-09-13
 */
@Getter
@Setter
@TableName("app_role_oauth_client")
public class AppRoleOauthClient extends BaseModel<AppRoleOauthClient> {

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
