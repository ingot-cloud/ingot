package com.ingot.cloud.member.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
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
@TenantTable
@TableName("member_user_social")
public class MemberUserSocial extends BaseModel<MemberUserSocial> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 组织ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 渠道类型
     */
    @TableField("`type`")
    private SocialTypeEnum type;

    /**
     * 渠道唯一ID
     */
    private String uniqueId;

    /**
     * 绑定时间
     */
    private LocalDateTime bindAt;
}
