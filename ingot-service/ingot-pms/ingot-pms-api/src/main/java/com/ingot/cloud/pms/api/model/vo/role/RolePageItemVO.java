package com.ingot.cloud.pms.api.model.vo.role;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.ingot.cloud.pms.api.model.enums.RoleTypeEnums;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
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
     * 角色编码
     */
    private String code;
    /**
     * 角色类型
     */
    private RoleTypeEnums type;
    /**
     * 角色状态
     */
    private CommonStatusEnum status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建日期
     */
    private LocalDateTime createdAt;
    /**
     * 删除日期
     */
    private LocalDateTime deletedAt;
    /**
     * 是否可以操作
     */
    private boolean canAction = true;
}
