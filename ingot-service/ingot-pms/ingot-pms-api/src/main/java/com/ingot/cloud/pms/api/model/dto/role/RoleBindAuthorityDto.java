package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : RoleBindAuthorityDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/13.</p>
 * <p>Time         : 2:58 PM.</p>
 */
@Data
public class RoleBindAuthorityDto implements Serializable {

    @NotBlank(message = "角色id不能为空")
    private String role_id;

    private List<Long> authority_ids;

    private List<Long> delete_authority_ids;
}
