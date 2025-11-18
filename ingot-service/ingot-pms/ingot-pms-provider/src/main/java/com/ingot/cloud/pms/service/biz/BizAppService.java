package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaApp;

/**
 * <p>Description  : BizAppService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 14:21.</p>
 */
public interface BizAppService {

    /**
     * 获取当前可用的应用列表，受租户限制
     *
     * @return {@link MetaApp}
     */
    List<MetaApp> getEnabledApps();

    /**
     * 获取当前禁用列表，受租户限制
     *
     * @return {@link MetaApp}
     */
    List<MetaApp> getDisabledApps();
}
