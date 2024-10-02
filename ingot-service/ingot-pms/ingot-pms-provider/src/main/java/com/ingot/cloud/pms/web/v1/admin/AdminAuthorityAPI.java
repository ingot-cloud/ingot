package com.ingot.cloud.pms.web.v1.admin;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.RequiredAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : AuthorityApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:12 下午.</p>
 */
@RestController
@RequestMapping(value = "/v1/admin/authority")
@RequiredArgsConstructor
public class AdminAuthorityAPI implements RShortcuts {
    private final SysAuthorityService sysAuthorityService;

    @RequiredAdmin
    @GetMapping("/tree")
    public R<?> tree(AuthorityFilterDTO filter) {
        return ok(sysAuthorityService.treeList(filter));
    }

    @RequiredAdmin
    @PostMapping
    public R<?> create(@RequestBody @Validated(Group.Create.class) SysAuthority params) {
        sysAuthorityService.createAuthority(params, true);
        return ok();
    }

    @RequiredAdmin
    @PutMapping
    public R<?> update(@RequestBody @Validated(Group.Update.class) SysAuthority params) {
        sysAuthorityService.updateAuthority(params);
        return ok();
    }

    @RequiredAdmin
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysAuthorityService.removeAuthorityById(id);
        return ok();
    }
}
