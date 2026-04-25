package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.TenantAppConfig;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.domain.PlatformAppService;
import com.ingot.cloud.pms.service.domain.TenantAppConfigService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizAppServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 14:23.</p>
 */
@Service
@RequiredArgsConstructor
public class BizAppServiceImpl implements BizAppService {
    private final PlatformAppService platformAppService;
    private final TenantAppConfigService tenantAppConfigService;

    @Override
    public List<PlatformApp> getEnabledApps() {
        // 获取当前组织私有配置
        List<TenantAppConfig> configs = tenantAppConfigService.list();

        return platformAppService.list()
                .stream()
                .filter(app -> {
                    // 如果存在私有配置，优先使用私有配置
                    TenantAppConfig appConfig = configs.stream()
                            .filter(config -> config.getAppId().equals(app.getId()))
                            .findFirst().orElse(null);
                    if (appConfig != null) {
                        return appConfig.getEnabled();
                    }
                    return app.getStatus() == CommonStatusEnum.ENABLE;
                })
                .toList();
    }

    @Override
    public List<PlatformApp> getDisabledApps() {
        // 获取当前组织私有配置
        List<TenantAppConfig> configs = tenantAppConfigService.list();

        return platformAppService.list()
                .stream()
                .filter(app -> {
                    // 如果存在私有配置，优先使用私有配置
                    TenantAppConfig appConfig = configs.stream()
                            .filter(config -> config.getAppId().equals(app.getId()))
                            .findFirst().orElse(null);
                    if (appConfig != null) {
                        return !appConfig.getEnabled();
                    }
                    return app.getStatus() == CommonStatusEnum.LOCK;
                })
                .toList();
    }
}
