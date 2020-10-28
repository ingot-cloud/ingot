package com.ingot.cloud.pms.api.model.vo.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.base.model.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : RoleWithUserVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/5.</p>
 * <p>Time         : 1:43 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleWithTargetUserIfPresentVo extends BaseVo {
    /**
     * 角色编码
     */
    private String role_code;

    /**
     * 角色名称
     */
    private String role_name;

    /**
     * 角色状态
     */
    private String status;

    /**
     * 角色备注
     */
    private String remark;

    /**
     * 角色类型
     */
    private String type;

    /**
     * 指定用户是否包含该角色
     */
    private boolean binding;

    /**
     * 用户 id
     */
    @JsonIgnore
    private Long user_id;

}