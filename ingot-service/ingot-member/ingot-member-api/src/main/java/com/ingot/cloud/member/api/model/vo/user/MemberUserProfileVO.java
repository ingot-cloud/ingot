package com.ingot.cloud.member.api.model.vo.user;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.sensitive.Sensitive;
import com.ingot.framework.core.utils.sensitive.SensitiveMode;
import com.ingot.framework.oss.common.OssUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>Description  : MemberUserProfileVO.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@Data
@Schema(description = "会员用户详情VO")
public class MemberUserProfileVO implements Serializable {
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    @Sensitive(mode = SensitiveMode.MOBILE_PHONE)
    private String phone;

    /**
     * 邮件地址
     */
    @Schema(description = "邮箱")
    @Sensitive(mode = SensitiveMode.EMAIL)
    private String email;

    /**
     * 头像
     */
    @Schema(description = "头像")
    @OssUrl
    private String avatar;

    /**
     * 状态, 0:正常，9:禁用
     */
    @Schema(description = "状态")
    private CommonStatusEnum status;

    /**
     * 用户角色列表
     */
    @Schema(description = "角色列表")
    private List<MemberRole> roles;

    /**
     * 创建日期
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

