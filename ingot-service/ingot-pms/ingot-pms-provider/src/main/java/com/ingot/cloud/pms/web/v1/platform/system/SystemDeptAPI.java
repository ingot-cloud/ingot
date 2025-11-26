package com.ingot.cloud.pms.web.v1.platform.system;

import java.util.List;

import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantEnv;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : SystemDeptAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/26.</p>
 * <p>Time         : 10:49.</p>
 */
@Slf4j
@Tag(description = "SystemDept", name = "系统部门管理模块")
@RestController
@RequestMapping(value = "/v1/platform/system/dept")
@RequiredArgsConstructor
public class SystemDeptAPI implements RShortcuts {
    private final BizDeptService bizDeptService;

    @Operation(summary = "部门树", description = "部门树")
    @AdminOrHasAnyAuthority({"platform:system:dept:query"})
    @GetMapping("/tree/{orgId}")
    public R<List<DeptTreeNodeVO>> tree(@PathVariable Long orgId) {
        return ok(TenantEnv.applyAs(orgId, bizDeptService::orgList));
    }
}
