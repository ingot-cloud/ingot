package com.ingot.cloud.member.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Getter
@Setter
@ToString
@TableName("member_social_details")
public class MemberSocialDetails extends BaseModel<MemberSocialDetails> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * App ID
     */
    private String appId;

    /**
     * App Secret
     */
    private String appSecret;

    /**
     * 重定向地址
     */
    private String redirectUrl;

    /**
     * 社交名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 类型
     */
    @TableField("`type`")
    private SocialTypeEnum type;

    /**
     * 状态, 0:正常，9:禁用
     */
    @TableField("`status`")
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
