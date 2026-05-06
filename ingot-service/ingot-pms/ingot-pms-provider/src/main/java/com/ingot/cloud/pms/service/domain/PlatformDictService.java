package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformDict;
import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.dto.dict.DictSortDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface PlatformDictService extends BaseService<PlatformDict> {

    /**
     * 全量列表，命中缓存
     *
     * @return 字典列表
     */
    List<PlatformDict> list();

    /**
     * 按查询条件返回平铺列表
     *
     * @param query 条件
     * @return 字典列表
     */
    List<PlatformDict> listByCondition(DictQueryDTO query);

    /**
     * 按查询条件分页
     *
     * @param page  分页
     * @param query 条件
     * @return 分页结果
     */
    IPage<PlatformDict> page(Page<PlatformDict> page, DictQueryDTO query);

    /**
     * 按 dictCode 获取作用域下生效的字典项（基于优先级合并）
     *
     * @param dictCode  字典编码
     * @param query     作用域条件
     * @return 字典项列表
     */
    List<PlatformDict> listItemsByCode(String dictCode, DictQueryDTO query);

    /**
     * 创建字典
     *
     * @param params {@link PlatformDict}
     */
    void create(PlatformDict params);

    /**
     * 更新字典
     *
     * @param params {@link PlatformDict}
     */
    void update(PlatformDict params);

    /**
     * 删除字典
     *
     * @param id 字典ID
     */
    void delete(long id);

    /**
     * 切换启停状态
     *
     * @param id     字典ID
     * @param status 目标状态
     */
    void changeStatus(long id, CommonStatusEnum status);

    /**
     * 批量更新排序
     *
     * @param items 排序项
     */
    void batchSort(List<DictSortDTO> items);
}
