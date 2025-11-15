package com.ingot.cloud.pms.api.model.vo.role;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import lombok.Data;

/**
 * <p>Description  : RoleItemVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/14.</p>
 * <p>Time         : 11:19.</p>
 */
@Data
public class RoleItemVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * PID
     */
    private Long pid;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色类型
     */
    private RoleTypeEnum type;

    /**
     * 组织类型
     */
    private OrgTypeEnum orgType;

    /**
     * 是否过滤部门
     */
    private Boolean filterDept;

    /**
     * 数据范围类型
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
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /* --- 扩展 --- */
    private String typeText;
    private String orgTypeText;
    private String scopeTypeText;
    private String statusText;
}
