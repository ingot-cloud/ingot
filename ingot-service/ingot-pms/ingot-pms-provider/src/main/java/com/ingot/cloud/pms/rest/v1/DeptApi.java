package com.ingot.cloud.pms.rest.v1;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.framework.core.validation.Group;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.R;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : DeptApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:05 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/dept")
@AllArgsConstructor
public class DeptApi extends BaseController {
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
    public R<?> removeById(@PathVariable Long id) {
        sysDeptService.removeDeptById(id);
        return ok();
    }
}
