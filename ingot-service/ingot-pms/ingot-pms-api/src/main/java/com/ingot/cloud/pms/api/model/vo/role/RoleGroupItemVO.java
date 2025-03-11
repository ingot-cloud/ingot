package com.ingot.cloud.pms.api.model.vo.role;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnums;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
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
     * 角色编码
     */
    private String code;
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
    private OrgTypeEnums type;
    /**
     * 是否过滤部门
     */
    private Boolean filterDept;
    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;
    /**
     * 角色列表
     */
    private List<RoleGroupItemVO> children;
}
