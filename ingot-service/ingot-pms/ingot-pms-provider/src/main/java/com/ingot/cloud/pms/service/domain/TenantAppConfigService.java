package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.TenantAppConfig;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface TenantAppConfigService extends BaseService<TenantAppConfig> {

    /**
     * 创建配置
     *
     * @param params {@link TenantAppConfig}
     */
    void create(TenantAppConfig params);

    /**
     * 更新配置
     *
     * @param params {@link TenantAppConfig}
     */
    void update(TenantAppConfig params);

    /**
     * 根据应用ID清除数据
     *
     * @param id 应用ID
     */
    void clearByAppId(long id);

    /**
     * 根据租户ID清除数据
     *
     * @param tenantId 租户ID
     */
    void clearByTenantId(long tenantId);
}
