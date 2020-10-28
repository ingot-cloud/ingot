package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * <p>Description  : RoleCreateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/24.</p>
 * <p>Time         : 上午10:10.</p>
 */
@Data
public class RoleCreateDto implements Serializable {
    /**
     * 角色编码
     */
    @NotEmpty(message = "角色编码不能为空")
    private String role_code;

    /**
     * 角色名称
     */
    @NotEmpty(message = "角色名称不能为空")
    private String role_name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 角色类型
     */
    private String type;
}
