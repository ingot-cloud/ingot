package com.ingot.cloud.pms.service.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;

/**
 * <p>Description  : BizPlatformAppService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/15.</p>
 * <p>Time         : 16:42.</p>
 */
public interface BizPlatformAppService {

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
