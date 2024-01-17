package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.dto.auth.SocialRegisterDTO;

/**
 * <p>Description  : LoginService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/17.</p>
 * <p>Time         : 13:44.</p>
 */
public interface LoginService {

    /**
     * 社交注册
     *
     * @param params {@link SocialRegisterDTO}
     */
    void register(SocialRegisterDTO params);
}
