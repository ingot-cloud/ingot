package com.ingot.framework.core.model.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * <p>Description  : UserDetailsDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:47 下午.</p>
 */
@Getter
@Builder
public class UserDetailsDto implements Serializable {
    /**
     * 客户端ID
     */
    private final String clientId;
    /**
     * 租户ID
     */
    private final String tenantID;
    /**
     * 授权类型
     */
    private final String type;
    /**
     * 唯一编码，根据类型判断，可以是用户名或手机号或社交openId
     */
    private final String uniqueCode;
}
