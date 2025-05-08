package com.ingot.cloud.pms.web.v1.org;

import com.ingot.cloud.pms.api.model.dto.dept.DeptWithManagerDTO;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(description = "orgDept", name = "组织部门模块")
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

    @AdminOrHasAnyAuthority({"contacts:member:r", "contacts:dept:w", "contacts:dept:r"})
    @GetMapping("/tree2")
    public R<?> tree2() {
        return ok(bizDeptService.orgTree());
    }

    @AdminOrHasAnyAuthority({"contacts:dept:w"})
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody DeptWithManagerDTO params) {
        bizDeptService.orgCreateDept(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"contacts:dept:w"})
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody DeptWithManagerDTO params) {
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
