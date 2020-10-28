package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : RoleBindApplicationDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/21.</p>
 * <p>Time         : 4:16 PM.</p>
 */
@Data
public class RoleBindApplicationDto implements Serializable {

    @NotBlank(message = "角色id不能为空")
    private String role_id;

    private List<String> application_ids;

    private List<String> delete_application_ids;
}
