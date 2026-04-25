package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface PlatformAppService extends BaseService<PlatformApp> {

    /**
     * 平台应用列表
     *
     * @param page      分页参数
     * @param condition 条件参数
     * @return {@link PlatformApp}
     */
    IPage<PlatformApp> conditionPage(Page<PlatformApp> page, PlatformApp condition);

    /**
     * 创建应用
     *
     * @param params {@link PlatformApp}
     */
    void create(PlatformApp params);

    /**
     * 更新应用
     *
     * @param params {@link PlatformApp}
     */
    void update(PlatformApp params);

    /**
     * 删除应用
     *
     * @param id 应用ID
     */
    void delete(long id);

}
