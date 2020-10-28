package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : RoleBindDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/29.</p>
 * <p>Time         : 11:37 AM.</p>
 */
@Data
public class RoleBindUserDto implements Serializable {

    @NotBlank(message = "角色id不能为空")
    private String role_id;

    private List<Long> user_ids;

    private List<Long> delete_user_ids;
}
