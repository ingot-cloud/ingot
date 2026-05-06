package com.ingot.cloud.pms.web.inner;

import java.util.List;
import java.util.Map;

import com.ingot.cloud.pms.api.model.dto.dict.DictQueryDTO;
import com.ingot.cloud.pms.api.model.vo.dict.DictItemVO;
import com.ingot.cloud.pms.service.biz.BizPlatformDictService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description : Dict Inner API，供其它微服务通过 RPC 读取字典数据。</p>
 *
 * @author jy
 * @since 2026/4/25
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/inner/dict")
@RequiredArgsConstructor
public class InnerDictAPI implements RShortcuts {
    private final BizPlatformDictService platformDictService;

    @PostMapping("/items")
    public R<List<DictItemVO>> items(@RequestParam("code") String code, @RequestBody(required = false) DictQueryDTO query) {
        return ok(platformDictService.items(code, query));
    }

    @PostMapping("/nodes")
    public R<List<DictItemVO>> nodes(@RequestParam("code") String code, @RequestBody(required = false) DictQueryDTO query) {
        return ok(platformDictService.nodes(code, query));
    }

    @PostMapping("/batch")
    public R<Map<String, List<DictItemVO>>> batchItems(@RequestParam("codes") List<String> codes,
                                                      @RequestBody(required = false) DictQueryDTO query) {
        return ok(platformDictService.batchItems(codes, query));
    }
}
