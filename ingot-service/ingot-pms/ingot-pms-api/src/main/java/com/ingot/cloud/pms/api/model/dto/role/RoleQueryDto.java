package com.ingot.cloud.pms.api.model.dto.role;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : RoleQueryDtp.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/1/14.</p>
 * <p>Time         : 8:38 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleQueryDto extends BaseQueryDto {
    /**
     * 角色类型
     */
    private List<String> typeList;

    /**
     * 角色名称
     */
    private String roleName;
}
