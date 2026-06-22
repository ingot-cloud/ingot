package com.ingot.cloud.pms.web.v1.platform.config;

import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.service.biz.BizPlatformMenuService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>平台菜单只读接口，提供全量菜单树供前端展示；增删改请使用应用中心化接口 {@link PlatformApplicationAPI}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(description = "PlatformMenu", name = "平台菜单查询")
@RequestMapping(value = "/v1/platform/config/menu")
@RequiredArgsConstructor
public class PlatformMenuAPI implements RShortcuts {
    private final BizPlatformMenuService menuService;

    @AdminOrHasAnyAuthority({"platform:config:menu:query"})
    @GetMapping("/tree")
    @Operation(summary = "菜单树", description = "返回全量菜单树，供前端展示所有菜单")
    public R<?> tree(PlatformMenu filter) {
        return ok(menuService.treeList(filter));
    }
}
