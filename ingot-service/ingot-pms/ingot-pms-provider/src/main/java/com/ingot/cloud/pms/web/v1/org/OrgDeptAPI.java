package com.ingot.cloud.pms.web.v1.org;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : OrgDeptAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/22.</p>
 * <p>Time         : 2:12 PM.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/org/dept")
@RequiredArgsConstructor
public class OrgDeptAPI implements RShortcuts {
    private final BizDeptService bizDeptService;

    @AdminOrHasAnyAuthority({"contacts:member:r", "contacts:dept:w", "contacts:dept:r"})
    @GetMapping("/tree")
    public R<?> tree() {
        return ok(bizDeptService.orgList());
    }

    @AdminOrHasAnyAuthority({"contacts:dept:w"})
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysDept params) {
        bizDeptService.orgCreateDept(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"contacts:dept:w"})
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysDept params) {
        bizDeptService.orgUpdateDept(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"contacts:dept:w"})
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizDeptService.orgDeleteDept(id);
        return ok();
    }
}
