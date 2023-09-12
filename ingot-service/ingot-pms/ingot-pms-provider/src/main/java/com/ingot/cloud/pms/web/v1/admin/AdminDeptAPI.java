package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : DeptApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:05 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/admin/dept")
@RequiredArgsConstructor
public class AdminDeptAPI implements RShortcuts {
    private final SysDeptService sysDeptService;

    @PreAuthorize("@ingot.hasAnyAuthority('basic.dept.write', 'basic.dept.read')")
    @GetMapping("/tree")
    public R<?> tree() {
        return ok(sysDeptService.treeList());
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.dept.write')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysDept params) {
        sysDeptService.createDept(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.dept.write')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysDept params) {
        sysDeptService.updateDept(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.dept.write')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysDeptService.removeDeptById(id);
        return ok();
    }
}
