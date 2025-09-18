package com.ingot.cloud.pms.api.model.dto.auth;

import java.io.Serializable;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.Data;

/**
 * <p>Description  : SocialRegisterDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/17.</p>
 * <p>Time         : 13:45.</p>
 */
@Data
public class SocialRegisterDTO implements Serializable {
    /**
     * 社交类型
     */
    private SocialTypeEnum type;
    /**
     * 社交码，社交唯一标识
     */
    private String code;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 昵称
     */
    private String nickname;
}
