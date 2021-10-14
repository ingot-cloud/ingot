package com.ingot.cloud.pms.api.model.dto.role;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>Description  : RoleAuthorityDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/10.</p>
 * <p>Time         : 下午1:43.</p>
 */
@Data
public class RoleAuthorityDto implements Serializable {
    private List<String> url_list;
}
