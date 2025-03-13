package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.config.TenantTable;
import com.ingot.framework.data.mybatis.model.BaseModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TenantTable
@TableName("sys_authority")
public class SysAuthority extends BaseModel<SysAuthority> implements AuthorityType {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 权限名称
     */
    @NotBlank(message = "{SysAuthority.name}", groups = Group.Create.class)
    private String name;

    /**
     * 权限编码
     */
    @NotBlank(message = "{SysAuthority.code}", groups = Group.Create.class)
    private String code;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 权限类型
     */
    private OrgTypeEnum type;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;


}
