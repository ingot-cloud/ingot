package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantEnv;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : AdminDeptAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/20.</p>
 * <p>Time         : 11:01 AM.</p>
 */
@Slf4j
@RestController
@Tag(description = "dept", name = "部门管理模块")
@RequestMapping(value = "/v1/admin/dept")
@RequiredArgsConstructor
public class AdminDeptAPI implements RShortcuts {
    private final SysDeptService sysDeptService;
    private final BizDeptService bizDeptService;

    @AdminOrHasAnyAuthority({"contacts:member:r", "contacts:dept:w", "contacts:dept:r"})
    @GetMapping("/tree/{orgId}")
    public R<?> tree(@PathVariable Long orgId) {
        return TenantEnv.applyAs(orgId, () -> ok(bizDeptService.orgList()));
    }

    @AdminOrHasAnyAuthority({"orgmanager:dept"})
    @GetMapping("/tree")
    public R<?> tree() {
        return ok(bizDeptService.orgList());
    }

    @AdminOrHasAnyAuthority({"orgmanager:dept"})
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysDept params) {
        params.setMainFlag(null);
        sysDeptService.createDept(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"orgmanager:dept"})
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysDept params) {
        params.setMainFlag(null);
        sysDeptService.updateDept(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"orgmanager:dept"})
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysDeptService.removeDeptById(id);
        return ok();
    }
}
