package com.ingot.cloud.member.api.model.dto.user;

import java.io.Serializable;

import com.ingot.framework.crypto.annotation.InFieldDecrypt;
import com.ingot.framework.crypto.model.CryptoType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>Description  : MemberUserPasswordDTO.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@Data
@Schema(description = "会员用户密码DTO")
public class MemberUserPasswordDTO implements Serializable {
    /**
     * 旧密码
     */
    @Schema(description = "旧密码")
    @InFieldDecrypt(CryptoType.AES)
    private String password;

    /**
     * 新密码
     */
    @Schema(description = "新密码")
    @InFieldDecrypt(CryptoType.AES)
    private String newPassword;
}

