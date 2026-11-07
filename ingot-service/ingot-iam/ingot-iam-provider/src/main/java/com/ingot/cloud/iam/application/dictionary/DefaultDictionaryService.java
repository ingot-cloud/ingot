package com.ingot.cloud.iam.application.dictionary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.infrastructure.persistence.entity.DictionaryEntity;
import com.ingot.cloud.iam.application.dictionary.model.DictionaryNodeInput;
import com.ingot.cloud.iam.application.dictionary.model.DictionaryNodeRecord;
import com.ingot.cloud.iam.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.iam.application.dictionary.model.DictSortDTO;
import com.ingot.cloud.iam.api.model.vo.dict.DictItemVO;
import com.ingot.cloud.iam.application.dictionary.model.DictTreeNodeVO;
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
public class DefaultDictionaryService implements DictionaryService {
    private final DictionaryNodeService dictService;
    private final DictionaryAssembler dictConvert;

    @Override
    public List<DictTreeNodeVO> tree(DictQueryDTO query) {
        List<DictionaryEntity> list = dictService.listByCondition(query);
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
        List<DictionaryEntity> typeList = dictService.listByCondition(copy);
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
        List<DictionaryEntity> children = dictService.listByCondition(childrenQuery)
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
    public IPage<DictionaryNodeRecord> page(Page<DictionaryNodeRecord> page, DictQueryDTO query) {
        IPage<DictionaryEntity> queryPage = page.convert(ignored -> null);
        return dictService.page(queryPage, query).convert(DictionaryNodeTransfer::toRecord);
    }

    @Override
    public void create(DictionaryNodeInput params) {
        dictService.create(DictionaryNodeTransfer.toEntity(params));
    }

    @Override
    public void update(DictionaryNodeInput params) {
        dictService.update(DictionaryNodeTransfer.toEntity(params));
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
