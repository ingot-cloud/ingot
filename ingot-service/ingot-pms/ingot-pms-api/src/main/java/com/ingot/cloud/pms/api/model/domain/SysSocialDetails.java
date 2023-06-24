package com.ingot.cloud.pms.api.model.domain;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.model.BaseModel;
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
@TableName("sys_social_details")
public class SysSocialDetails extends BaseModel<SysRoleAuthority> {

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
     * App ID
     */
    @NotBlank(message = "{SysSocialDetails.appId}", groups = Group.Create.class)
    private String appId;

    /**
     * App Secret
     */
    @NotBlank(message = "{SysSocialDetails.appSecret}", groups = Group.Create.class)
    private String appSecret;

    /**
     * 重定向地址
     */
    private String redirectUrl;

    /**
     * 社交名称
     */
    private String name;

    /**
     * 类型
     */
    @NotNull(message = "{SysSocialDetails.type}", groups = Group.Create.class)
    private SocialTypeEnums type;

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
