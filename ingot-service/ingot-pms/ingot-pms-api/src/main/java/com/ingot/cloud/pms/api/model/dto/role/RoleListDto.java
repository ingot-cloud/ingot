package com.ingot.cloud.pms.api.model.dto.role;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>Description  : RoleListDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/10.</p>
 * <p>Time         : 下午2:07.</p>
 */
@Data
public class RoleListDto implements Serializable {
    private List<String> role_list;
}
