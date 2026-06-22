package com.ingot.cloud.pms.web.v1.platform.config;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizPlatformPermissionService;
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
 * <p>平台权限只读接口，提供全量权限树供前端展示；增删改请使用应用中心化接口 {@link PlatformApplicationAPI}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(description = "PlatformPermission", name = "平台权限查询")
@RequestMapping(value = "/v1/platform/config/permission")
@RequiredArgsConstructor
public class PlatformPermissionAPI implements RShortcuts {
    private final BizPlatformPermissionService platformAuthorityService;

    @AdminOrHasAnyAuthority({"platform:config:permission:query"})
    @GetMapping("/tree")
    @Operation(summary = "权限树", description = "返回全量权限树，供前端展示所有权限")
    public R<List<PermissionTreeNodeVO>> tree(PlatformPermission params) {
        return ok(platformAuthorityService.treeList(params));
    }
}
