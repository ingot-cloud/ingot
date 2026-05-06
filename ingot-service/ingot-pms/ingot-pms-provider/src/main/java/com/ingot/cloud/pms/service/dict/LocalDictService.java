package com.ingot.cloud.pms.service.dict;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.service.biz.BizPlatformDictService;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.internal.DictItemAssembler;
import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.model.DictQuery;
import lombok.RequiredArgsConstructor;

/**
 * 本地字典实现，运行在 PMS 自身进程内，直接使用 {@link BizPlatformDictService}。
 *
 * @author jy
 * @since 2026/4/25
 */
@RequiredArgsConstructor
public class LocalDictService implements DictService {

    private final BizPlatformDictService bizDictService;

    @Override
    public List<DictItem> items(String dictCode, DictQuery query) {
        DictQueryDTO dto = DictItemAssembler.toQueryDTO(query);
        return DictItemAssembler.fromVOs(bizDictService.items(dictCode, dto));
    }

    @Override
    public Map<String, List<DictItem>> batchItems(List<String> dictCodes, DictQuery query) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            return Map.of();
        }
        DictQueryDTO dto = DictItemAssembler.toQueryDTO(query);
        Map<String, List<DictItem>> result = new LinkedHashMap<>();
        bizDictService.batchItems(dictCodes, dto)
                .forEach((code, list) -> result.put(code, DictItemAssembler.fromVOs(list)));
        return result;
    }
}
