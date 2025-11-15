package com.ingot.cloud.pms.service.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.MetaApp;

/**
 * <p>Description  : BizMetaAppService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/15.</p>
 * <p>Time         : 16:42.</p>
 */
public interface BizMetaAppService {

    /**
     * 元数据应用列表
     *
     * @param page      分页参数
     * @param condition 条件参数
     * @return {@link MetaApp}
     */
    IPage<MetaApp> conditionPage(Page<MetaApp> page, MetaApp condition);

    /**
     * 创建应用
     *
     * @param params {@link MetaApp}
     */
    void create(MetaApp params);

    /**
     * 更新应用
     *
     * @param params {@link MetaApp}
     */
    void update(MetaApp params);

    /**
     * 删除应用
     *
     * @param id 应用ID
     */
    void delete(long id);
}
