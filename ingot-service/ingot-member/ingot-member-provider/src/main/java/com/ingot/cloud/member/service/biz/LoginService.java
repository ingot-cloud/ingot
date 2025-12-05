package com.ingot.cloud.member.service.biz;

import com.ingot.cloud.pms.api.model.dto.auth.MiniProgramRegisterDTO;
import com.ingot.cloud.pms.api.model.dto.auth.SocialRegisterDTO;

/**
 * <p>Description  : LoginService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 17:15.</p>
 */
public interface LoginService {

    /**
     * 社交注册
     *
     * @param params {@link SocialRegisterDTO}
     */
    void register(SocialRegisterDTO params);

    /**
     * 小程序注册
     *
     * @param params {@link MiniProgramRegisterDTO}
     */
    void miniProgramRegister(MiniProgramRegisterDTO params);
}
