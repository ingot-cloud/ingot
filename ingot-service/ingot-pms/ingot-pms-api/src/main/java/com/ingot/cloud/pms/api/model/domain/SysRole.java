package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ingot.framework.core.validation.Group;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.validation.annotation.CommonStatusValidate;
import com.ingot.framework.store.mybatis.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
@TableName("sys_role")
public class SysRole extends BaseModel<SysRole> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    private Long id;

    /**
     * 角色名称
     */
    @NotBlank(message = "{SysRole.name}", groups = Group.Create.class)
    private String name;

    /**
     * 角色编码
     */
    @NotBlank(message = "{SysRole.code}", groups = Group.Create.class)
    private String code;

    /**
     * 角色类型
     */
    private String type;

    /**
     * 状态, 0:正常，9:禁用
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
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;


}
