package com.ingot.cloud.pms.api.model.vo.client;

import com.ingot.cloud.pms.api.model.vo.role.RoleVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : OAuthClientWithRoleVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/3.</p>
 * <p>Time         : 1:57 PM.</p>
 */
@Data
public class OAuthClientWithRoleVo implements Serializable {

    /**
     * Client ID
     */
    private String client_id;

    /**
     * 权限
     */
    private String authorities;

    /**
     * 描述
     */
    private String description;

    /**
     * 授权类型，默认standard
     */
    private String auth_type;

    /**
     * client类型
     */
    private String type;

    /**
     * Role List
     */
    private List<RoleVo> role_list;
}
