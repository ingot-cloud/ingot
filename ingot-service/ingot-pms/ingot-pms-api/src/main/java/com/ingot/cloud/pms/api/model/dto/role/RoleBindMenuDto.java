package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : RoleBindMenuDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/14.</p>
 * <p>Time         : 5:17 PM.</p>
 */
@Data
public class RoleBindMenuDto implements Serializable {

    @NotBlank(message = "角色id不能为空")
    private String role_id;

    private List<Long> menu_ids;

    private List<Long> delete_menu_ids;
}
