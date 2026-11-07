package com.ingot.cloud.iam.application.social.model;

import java.io.Serial;
import java.time.LocalDateTime;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.core.utils.validation.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.Data;

/**
 * <p>承载SocialConfig维护输入，沿用既有字段及校验语义。
 * </p>
 * @author jy
 * @since 1.0.0
 */
@Data
public class SocialConfigInput implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
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
    private LocalDateTime deletedAt;

}
