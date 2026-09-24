package com.ingot.cloud.iam.web.v1.platform;

import java.util.List;

import com.ingot.cloud.iam.catalog.CatalogService;
import com.ingot.framework.commons.model.iam.ActionLookupInput;
import com.ingot.framework.commons.model.iam.ActionLookupRecord;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>按操作 ID 解析名称与归属，供权限回显，不翻页拼装。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台操作解析")
@RequestMapping("/v1/platform/actions")
@RequiredArgsConstructor
public class PlatformActionAPI implements RShortcuts {
    private final CatalogService catalog;

    /**
     * 按操作 ID 批量解析应用、资源与范围能力。
     *
     * @param input 操作 ID
     * @return 解析结果；未命中的 ID 省略
     */
    @Operation(summary = "解析操作")
    @PostMapping("/lookup")
    public R<List<ActionLookupRecord>> lookup(@Valid @RequestBody ActionLookupInput input) {
        return ok(catalog.lookupActions(input));
    }
}
