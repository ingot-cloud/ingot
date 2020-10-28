package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * <p>Description  : RoleDeleteDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/24.</p>
 * <p>Time         : 上午10:16.</p>
 */
@Data
public class RoleDeleteDto implements Serializable {

    @NotEmpty(message = "角色id不能为空")
    private String id;
}
