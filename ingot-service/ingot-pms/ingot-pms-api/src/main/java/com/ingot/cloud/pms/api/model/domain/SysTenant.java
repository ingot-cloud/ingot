package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import com.ingot.framework.oss.common.OssSaveUrl;
import com.ingot.framework.oss.common.OssUrl;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@TableName("sys_tenant")
public class SysTenant extends BaseModel<SysTenant> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    private Long id;

    /**
     * 租户名称
     */
    @NotNull(message = "{SysTenant.name}", groups = Group.Create.class)
    private String name;

    /**
     * 租户编号
     */
    @NotNull(message = "{SysTenant.code}", groups = Group.Create.class)
    private String code;

    /**
     * 组织类型
     */
    private OrgTypeEnum orgType;

    /**
     * 头像
     */
    @OssSaveUrl
    @OssUrl
    private String avatar;

    /**
     * 计划ID
     */
    private Long planId;

    /**
     * 结束日期
     */
    private LocalDate endAt;

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

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;


}
