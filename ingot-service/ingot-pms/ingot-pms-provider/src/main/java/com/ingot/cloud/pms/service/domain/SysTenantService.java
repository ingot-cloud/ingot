package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.framework.store.mybatis.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysTenantService extends BaseService<SysTenant> {

    /**
     * 条件分页查询
     *
     * @param params 条件
     * @return 返回分页数据
     */
    IPage<SysTenant> conditionPage(Page<SysTenant> page, SysTenant params);

    /**
     * 创建租户
     *
     * @param params 参数
     */
    void createTenant(SysTenant params);

    /**
     * 删除租户
     *
     * @param id 租户ID
     */
    void removeTenantById(int id);

    /**
     * 根据ID更新租户
     *
     * @param params 参数
     */
    void updateTenantById(SysTenant params);
}
