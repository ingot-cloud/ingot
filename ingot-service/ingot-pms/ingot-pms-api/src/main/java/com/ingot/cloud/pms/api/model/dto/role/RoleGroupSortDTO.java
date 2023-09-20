package com.ingot.cloud.pms.api.model.dto.role;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : RoleGroupSortDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/20.</p>
 * <p>Time         : 3:09 PM.</p>
 */
@Data
public class RoleGroupSortDTO implements Serializable {
    private List<Long> ids;
}
