package com.ingot.cloud.member.api.model.dto.user;

import java.io.Serializable;
import java.util.List;

import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.crypto.annotation.InFieldEncrypt;
import com.ingot.framework.crypto.model.CryptoType;
import lombok.Data;

/**
 * <p>Description  : MemberUserInfoDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/5.</p>
 * <p>Time         : 10:42.</p>
 */
@Data
public class MemberUserInfoDTO implements Serializable {
    /**
     * 用户信息
     */
    private MemberUserBaseInfoDTO user;
    /**
     * 拥有角色
     */
    @InFieldEncrypt(CryptoType.AES)
    private List<String> roles;
    /**
     * 可以访问的租户
     */
    private List<AllowTenantDTO> allows;
}
