package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : RoleBindRoleDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/5.</p>
 * <p>Time         : 11:38 AM.</p>
 */
@Data
public class RoleBindRoleDto implements Serializable {

    @NotBlank(message = "用户id不能为空")
    private String user_id;

    private List<Long> role_ids;

    private List<Long> delete_role_ids;
}
