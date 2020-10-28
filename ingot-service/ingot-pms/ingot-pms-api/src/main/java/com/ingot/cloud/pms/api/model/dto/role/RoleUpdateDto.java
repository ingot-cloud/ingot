package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * <p>Description  : RoleUpdateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/24.</p>
 * <p>Time         : 上午10:17.</p>
 */
@Data
public class RoleUpdateDto implements Serializable {

    @NotEmpty(message = "角色id不能为空")
    private String id;

    /**
     * 角色编码
     */
    private String role_code;

    /**
     * 角色名称
     */
    private String role_name;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 角色类型
     */
    private String type;
}
