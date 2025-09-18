package com.ingot.cloud.pms.api.model.vo.role;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import lombok.Data;

/**
 * <p>Description  : RolePageItemVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/22.</p>
 * <p>Time         : 3:30 下午.</p>
 */
@Data
public class RolePageItemVO implements Serializable {
    /**
     * 角色ID
     */
    private Long id;
    /**
     * 角色名称
     */
    private String name;
    /**
     * 组ID
     */
    private Long groupId;
    /**
     * 组名称
     */
    private String groupName;
    /**
     * 角色编码
     */
    private String code;
    /**
     * 角色类型
     */
    private OrgTypeEnum type;
    /**
     * 是否过滤部门
     */
    private Boolean filterDept;
    /**
     * 角色状态
     */
    private CommonStatusEnum status;
    /**
     * 创建日期
     */
    private LocalDateTime createdAt;
    /**
     * 删除日期
     */
    private LocalDateTime deletedAt;
}
