package com.ingot.cloud.pms.web.v1;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.framework.core.validation.Group;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.R;
import lombok.RequiredArgsConstructor;
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
 * <p>Description  : AuthorityApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:12 下午.</p>
 */
@RestController
@RequestMapping(value = "/v1/authority")
@RequiredArgsConstructor
public class AuthorityApi extends BaseController {
    private final SysAuthorityService sysAuthorityService;

    @GetMapping("/tree")
    public R<?> tree() {
        return ok(sysAuthorityService.tree());
    }

    @PostMapping
    public R<?> create(@RequestBody @Validated(Group.Create.class) SysAuthority params) {
        sysAuthorityService.createAuthority(params);
        return ok();
    }

    @PutMapping
    public R<?> update(@RequestBody @Validated(Group.Update.class) SysAuthority params) {
        sysAuthorityService.updateAuthority(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Integer id) {
        sysAuthorityService.removeAuthorityById(id);
        return ok();
    }
}
