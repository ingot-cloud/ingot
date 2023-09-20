package com.ingot.cloud.pms.api.model.vo.role;

import com.ingot.cloud.pms.api.model.enums.RoleTypeEnums;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : RoleGroupItemVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/20.</p>
 * <p>Time         : 2:20 PM.</p>
 */
@Data
public class RoleGroupItemVO implements Serializable {
    /**
     * ID
     */
    private Long id;
    /**
     * 名称
     */
    private String name;
    /**
     * 是否为角色组
     */
    private Boolean isGroup;
    /**
     * 角色组ID，非角色组情况，不为空
     */
    private Long groupId;
    /**
     * 类型
     */
    private RoleTypeEnums type;
    /**
     * 角色列表
     */
    private List<RoleGroupItemVO> children;
}
