package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;

/**
 * <p>Description  : SocialDetailsService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 16:04.</p>
 */
public interface SocialDetailsService {

    /**
     * 根据类型获取社交信息
     *
     * @param type {@link SocialTypeEnum}
     * @return {@link SysSocialDetails}
     */
    List<SysSocialDetails> getSocialDetailsByType(SocialTypeEnum type);
}
