package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.security.common.constants.UserType;
import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserDetailsRequest.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:47 下午.</p>
 */
@Data
public class UserDetailsRequest implements Serializable {
    /**
     * 唯一编码，根据类型判断，可以是用户名或手机号或社交openId等
     */
    private String username;
    /**
     * 授权类型
     */
    private String grantType;
    /**
     * 用户类型
     */
    private UserType userType;
    /**
     * 登录的tenant，可以为空
     */
    private Long tenant;
    /**
     * 社交类型, {@code grantType} 为 {@link IngotAuthorizationGrantType#SOCIAL}时，不为空
     */
    private SocialTypeEnums socialType;
    /**
     * 社交code, {@code grantType} 为 {@link IngotAuthorizationGrantType#SOCIAL}时，不为空
     */
    private String socialCode;
}
