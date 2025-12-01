package com.ingot.cloud.member.api.model.dto.user;

import java.io.Serializable;

import com.ingot.framework.oss.common.OssSaveUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>Description  : MemberUserBaseInfoDTO.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@Data
@Schema(description = "会员用户基本信息DTO")
public class MemberUserBaseInfoDTO implements Serializable {
    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮件地址
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 头像
     */
    @Schema(description = "头像")
    @OssSaveUrl
    private String avatar;
}

