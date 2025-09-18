package com.ingot.cloud.pms.api.model.vo.role;

import java.io.Serializable;
import java.util.List;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import lombok.Data;

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
    private OrgTypeEnum type;
    /**
     * 是否过滤部门
     */
    private Boolean filterDept;
    /**
     * 数据权限类型
     */
    private DataScopeTypeEnum scopeType;
    /**
     * 数据权限范围
     */
    private List<Long> scopes;
    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;
    /**
     * 角色列表
     */
    private List<RoleGroupItemVO> children;
}
