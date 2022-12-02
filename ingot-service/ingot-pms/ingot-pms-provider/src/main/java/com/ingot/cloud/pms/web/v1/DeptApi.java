package com.ingot.cloud.pms.web.v1;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping(value = "/v1/dept")
@RequiredArgsConstructor
public class DeptApi implements RShortcuts {
    private final SysDeptService sysDeptService;

    @GetMapping("/tree")
    public R<?> tree() {
        return ok(sysDeptService.tree());
    }

    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody SysDept params) {
        sysDeptService.createDept(params);
        return ok();
    }

    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody SysDept params) {
        sysDeptService.updateDept(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Integer id) {
        sysDeptService.removeDeptById(id);
        return ok();
    }
}
