package com.ingot.cloud.pms.service.biz.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.convert.DictConvert;
import com.ingot.cloud.pms.api.model.domain.PlatformDict;
import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.dto.dict.DictSortDTO;
import com.ingot.cloud.pms.api.model.vo.dict.DictItemVO;
import com.ingot.cloud.pms.api.model.vo.dict.DictTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizPlatformDictService;
import com.ingot.cloud.pms.service.domain.PlatformDictService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 字典业务服务实现。
 *
 * @author jy
 * @since 2026/4/25
 */
@Service
@RequiredArgsConstructor
public class BizPlatformDictServiceImpl implements BizPlatformDictService {
    private final PlatformDictService dictService;
    private final DictConvert dictConvert;

    @Override
    public List<DictTreeNodeVO> tree(DictQueryDTO query) {
        List<PlatformDict> list = dictService.listByCondition(query);
        List<DictTreeNodeVO> nodes = dictConvert.toTreeNodes(list);
        List<DictTreeNodeVO> tree = TreeUtil.build(nodes);
        TreeUtil.compensate(tree, nodes);
        return tree;
    }

    @Override
    public List<DictItemVO> items(String dictCode, DictQueryDTO query) {
        return dictConvert.toItems(dictService.listItemsByCode(dictCode, query));
    }

    @Override
    public List<DictItemVO> nodes(String dictCode, DictQueryDTO query) {
        DictQueryDTO copy = new DictQueryDTO();
        if (query != null) {
            copy.setScopeType(query.getScopeType());
            copy.setTenantId(query.getTenantId());
            copy.setAppId(query.getAppId());
            copy.setOrgType(query.getOrgType());
            copy.setStatus(query.getStatus());
        }
        copy.setCode(dictCode);
        List<PlatformDict> typeList = dictService.listByCondition(copy);
        if (typeList.isEmpty()) {
            return List.of();
        }
        // 取首个匹配的类型节点的所有子项（不含跨作用域合并）
        Long typeId = typeList.get(0).getId();
        DictQueryDTO childrenQuery = new DictQueryDTO();
        if (query != null) {
            childrenQuery.setScopeType(query.getScopeType());
            childrenQuery.setTenantId(query.getTenantId());
            childrenQuery.setAppId(query.getAppId());
            childrenQuery.setOrgType(query.getOrgType());
            childrenQuery.setStatus(query.getStatus());
        }
        List<PlatformDict> children = dictService.listByCondition(childrenQuery)
                .stream()
                .filter(item -> typeId.equals(item.getPid()))
                .toList();
        return dictConvert.toItems(children);
    }

    @Override
    public Map<String, List<DictItemVO>> batchItems(List<String> dictCodes, DictQueryDTO query) {
        if (CollUtil.isEmpty(dictCodes)) {
            return Map.of();
        }
        Map<String, List<DictItemVO>> result = new LinkedHashMap<>();
        for (String code : dictCodes) {
            result.put(code, dictConvert.toItems(dictService.listItemsByCode(code, query)));
        }
        return result;
    }

    @Override
    public IPage<PlatformDict> page(Page<PlatformDict> page, DictQueryDTO query) {
        return dictService.page(page, query);
    }

    @Override
    public void create(PlatformDict params) {
        dictService.create(params);
    }

    @Override
    public void update(PlatformDict params) {
        dictService.update(params);
    }

    @Override
    public void delete(long id) {
        dictService.delete(id);
    }

    @Override
    public void changeStatus(long id, CommonStatusEnum status) {
        dictService.changeStatus(id, status);
    }

    @Override
    public void batchSort(List<DictSortDTO> items) {
        dictService.batchSort(items);
    }
}
