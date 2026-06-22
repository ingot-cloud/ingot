package com.ingot.cloud.pms.service.biz.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.TenantAppConfig;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.domain.PlatformAppService;
import com.ingot.cloud.pms.service.domain.TenantAppConfigService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BizAppServiceImpl implements BizAppService {
    private final PlatformAppService platformAppService;
    private final TenantAppConfigService tenantAppConfigService;

    @Override
    public List<PlatformApp> getEnabledApps() {
        List<TenantAppConfig> configs = tenantAppConfigService.list();
        LocalDateTime now = LocalDateTime.now();

        return platformAppService.list()
                .stream()
                .filter(app -> app.getStatus() == CommonStatusEnum.ENABLE)
                .filter(app -> app.getAppType() == null || app.getAppType() == OrgTypeEnum.Tenant)
                .filter(app -> isTenantAppEnabled(app, configs, now))
                .toList();
    }

    @Override
    public List<PlatformApp> getDisabledApps() {
        List<TenantAppConfig> configs = tenantAppConfigService.list();
        LocalDateTime now = LocalDateTime.now();

        return platformAppService.list()
                .stream()
                .filter(app -> app.getAppType() == null || app.getAppType() == OrgTypeEnum.Tenant)
                .filter(app -> !isTenantAppEnabled(app, configs, now)
                        || app.getStatus() == CommonStatusEnum.LOCK)
                .toList();
    }

    private boolean isTenantAppEnabled(PlatformApp app,
                                       List<TenantAppConfig> configs,
                                       LocalDateTime now) {
        TenantAppConfig appConfig = configs.stream()
                .filter(config -> config.getAppId().equals(app.getId()))
                .findFirst()
                .orElse(null);
        if (appConfig == null) {
            return app.getStatus() == CommonStatusEnum.ENABLE;
        }
        if (!Boolean.TRUE.equals(appConfig.getEnabled())) {
            return false;
        }
        if (appConfig.getValidFrom() != null && now.isBefore(appConfig.getValidFrom())) {
            return false;
        }
        if (appConfig.getValidUntil() != null && now.isAfter(appConfig.getValidUntil())) {
            return false;
        }
        return app.getStatus() == CommonStatusEnum.ENABLE;
    }
}
