package com.ingot.cloud.iam.infrastructure.persistence.entity;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>持久化平台全局及已有租户的社交渠道配置。</p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_social_config")
public class SocialConfigEntity extends BaseModel<SocialConfigEntity> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    private Long id;

    /**
     * 已有配置的租户归属，平台全局配置为空，普通配置更新不改归属。
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
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
    private SocialTypeEnum type;

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
