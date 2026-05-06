package com.ingot.cloud.pms.api.rpc;

import java.util.List;
import java.util.Map;

import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.vo.dict.DictItemVO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * PMS 字典 Feign 接口。其它微服务通过此接口读取平台字典数据。
 *
 * @author jy
 * @since 2026/4/25
 */
@FeignClient(contextId = "pmsDictService", value = ServiceNameConstants.PMS_SERVICE)
public interface RemotePmsDictService {

    /**
     * 按 code 查询作用域生效的字典项。
     *
     * @param code  字典编码
     * @param query 作用域条件，可包含 scopeType/tenantId/appId/orgType
     * @return 字典项列表
     */
    @PostMapping("/inner/dict/items")
    R<List<DictItemVO>> items(@RequestParam("code") String code, @RequestBody DictQueryDTO query);

    /**
     * 按 code 查询字典节点（不做作用域合并），用于需要明确单作用域数据的场景。
     */
    @PostMapping("/inner/dict/nodes")
    R<List<DictItemVO>> nodes(@RequestParam("code") String code, @RequestBody DictQueryDTO query);

    /**
     * 批量按 code 查询字典项，避免高频字典页面的多次 RPC。
     *
     * @return 以 dictCode 为 key、字典项列表为 value 的 Map
     */
    @PostMapping("/inner/dict/batch")
    R<Map<String, List<DictItemVO>>> batchItems(@RequestParam("codes") List<String> codes,
                                                @RequestBody DictQueryDTO query);
}
