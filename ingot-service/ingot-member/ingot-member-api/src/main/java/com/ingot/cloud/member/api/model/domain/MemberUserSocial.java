package com.ingot.cloud.member.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.model.BaseModel;

import java.time.LocalDateTime;
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
@TableName("member_user_social")
public class MemberUserSocial extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
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
    private String type;

    /**
     * 渠道唯一ID
     */
    private String uniqueId;

    /**
     * 绑定时间
     */
    private LocalDateTime bindAt;
}
