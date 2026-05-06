package com.ingot.cloud.pms.service.biz;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformDict;
import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.dto.dict.DictSortDTO;
import com.ingot.cloud.pms.api.model.vo.dict.DictItemVO;
import com.ingot.cloud.pms.api.model.vo.dict.DictTreeNodeVO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;

/**
 * 字典业务服务。
 *
 * @author jy
 * @since 2026/4/25
 */
public interface BizPlatformDictService {

    /**
     * 字典树（含字典类型与字典项）
     *
     * @param query 查询条件
     * @return 树列表
     */
    List<DictTreeNodeVO> tree(DictQueryDTO query);

    /**
     * 按 dictCode 查询作用域生效的字典项（启用且按排序）
     *
     * @param dictCode 字典编码
     * @param query    作用域条件
     * @return 字典项列表
     */
    List<DictItemVO> items(String dictCode, DictQueryDTO query);

    /**
     * 按 dictCode 查询完整字典节点（含 TYPE 与 ITEM）
     *
     * @param dictCode 字典编码
     * @param query    作用域条件
     * @return 字典项列表
     */
    List<DictItemVO> nodes(String dictCode, DictQueryDTO query);

    /**
     * 批量按 dictCode 查询字典项
     *
     * @param dictCodes 字典编码集合
     * @param query     作用域条件
     * @return 以 dictCode 为 key、字典项列表为 value 的 Map
     */
    Map<String, List<DictItemVO>> batchItems(List<String> dictCodes, DictQueryDTO query);

    /**
     * 管理端分页
     *
     * @param page  分页参数
     * @param query 条件
     * @return 分页
     */
    IPage<PlatformDict> page(Page<PlatformDict> page, DictQueryDTO query);

    /**
     * 创建
     */
    void create(PlatformDict params);

    /**
     * 更新
     */
    void update(PlatformDict params);

    /**
     * 删除
     */
    void delete(long id);

    /**
     * 启停
     */
    void changeStatus(long id, CommonStatusEnum status);

    /**
     * 批量排序
     */
    void batchSort(List<DictSortDTO> items);
}
