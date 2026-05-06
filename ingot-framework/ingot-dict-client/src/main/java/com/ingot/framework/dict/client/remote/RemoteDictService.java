package com.ingot.framework.dict.client.remote;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.vo.dict.DictItemVO;
import com.ingot.cloud.pms.api.rpc.RemotePmsDictService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.internal.DictItemAssembler;
import com.ingot.framework.dict.client.model.DictItem;
import com.ingot.framework.dict.client.model.DictQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 {@link RemotePmsDictService} 的字典 RPC 实现，由 dict-client 自动配置在
 * 非 PMS 服务中注册。
 *
 * @author jy
 * @since 2026/4/25
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteDictService implements DictService {

    private final RemotePmsDictService remotePmsDictService;

    @Override
    public List<DictItem> items(String dictCode, DictQuery query) {
        DictQueryDTO dto = DictItemAssembler.toQueryDTO(query);
        try {
            R<List<DictItemVO>> response = remotePmsDictService.items(dictCode, dto);
            if (response == null || !response.isSuccess()) {
                log.warn("[Dict] Remote call failed for dictCode={}, response={}", dictCode, response);
                return List.of();
            }
            return DictItemAssembler.fromVOs(response.getData());
        } catch (Exception e) {
            log.warn("[Dict] Remote call error for dictCode={}", dictCode, e);
            return List.of();
        }
    }

    @Override
    public Map<String, List<DictItem>> batchItems(List<String> dictCodes, DictQuery query) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            return Map.of();
        }
        DictQueryDTO dto = DictItemAssembler.toQueryDTO(query);
        try {
            R<Map<String, List<DictItemVO>>> response = remotePmsDictService.batchItems(dictCodes, dto);
            if (response == null || !response.isSuccess()) {
                log.warn("[Dict] Remote batch call failed for dictCodes={}, response={}", dictCodes, response);
                return defaultEmpty(dictCodes);
            }
            Map<String, List<DictItemVO>> data = response.getData();
            if (data == null) {
                return defaultEmpty(dictCodes);
            }
            Map<String, List<DictItem>> result = new LinkedHashMap<>();
            for (String code : dictCodes) {
                List<DictItemVO> items = data.get(code);
                result.put(code, items == null ? List.of() : DictItemAssembler.fromVOs(items));
            }
            return result;
        } catch (Exception e) {
            log.warn("[Dict] Remote batch call error for dictCodes={}", dictCodes, e);
            return defaultEmpty(dictCodes);
        }
    }

    private static Map<String, List<DictItem>> defaultEmpty(List<String> dictCodes) {
        Map<String, List<DictItem>> result = new LinkedHashMap<>();
        for (String code : dictCodes) {
            result.put(code, List.of());
        }
        return result;
    }
}
