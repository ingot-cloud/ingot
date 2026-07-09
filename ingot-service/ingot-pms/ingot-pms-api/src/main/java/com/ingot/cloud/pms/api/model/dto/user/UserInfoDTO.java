package com.ingot.cloud.pms.api.model.dto.user;

import java.io.Serializable;
import java.util.List;

import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.security.crypto.annotation.InEncryptField;
import com.ingot.framework.security.credential.model.CredentialErrorCode;
import lombok.Data;

/**
 * <p>Description  : UserInfoDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/6.</p>
 * <p>Time         : 11:36 上午.</p>
 */
@Data
public class UserInfoDTO implements Serializable {
    /**
     * 用户信息
     */
    private UserBaseInfoDTO user;
    /**
     * 拥有角色
     */
    @InEncryptField
    private List<String> roles;
    /**
     * 可以访问的租户
     */
    private List<TenantMainDTO> allows;
    /**
     * 必须修改密码
     */
    @InEncryptField
    private Boolean mustChangePwd;
    /**
     * 凭证状态码，用于前端展示过期预警。缺省 null 表示无需提示。
     * <ul>
     *   <li>{@code "pwd_expiring_soon"}：密码即将过期，配合 {@link #daysLeft}</li>
     *   <li>{@code "pwd_expired_with_grace"}：密码已过期但在宽限期内，配合 {@link #graceRemaining}</li>
     * </ul>
     */
    private CredentialErrorCode credentialStatus;
    /**
     * 距密码过期剩余天数（仅 {@code credentialStatus="pwd_expiring_soon"} 时有值）
     */
    private Long daysLeft;
    /**
     * 宽限期剩余登录次数（仅 {@code credentialStatus="pwd_expired_with_grace"} 时有值）
     */
    private Integer graceRemaining;
}
