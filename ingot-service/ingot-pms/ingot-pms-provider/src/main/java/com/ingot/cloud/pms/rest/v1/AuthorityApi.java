package com.ingot.cloud.pms.rest.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.framework.core.validation.Group;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : AuthorityApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:12 下午.</p>
 */
@RestController
@RequestMapping(value = "/v1/authority")
@AllArgsConstructor
public class AuthorityApi extends BaseController {
    private final SysAuthorityService sysAuthorityService;

    @GetMapping("/page")
    public IngotResponse<?> page(Page<SysAuthority> page, SysAuthority params) {
        return ok(sysAuthorityService.page(page, Wrappers.lambdaQuery(params)
                .isNull(params.getPid() == null, SysAuthority::getPid)));
    }

    @PostMapping
    public IngotResponse<?> create(@RequestBody @Validated(Group.Create.class) SysAuthority params) {
        sysAuthorityService.createAuthority(params);
        return ok();
    }

    @PutMapping
    public IngotResponse<?> update(@RequestBody @Validated(Group.Update.class) SysAuthority params) {
        sysAuthorityService.updateAuthority(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public IngotResponse<?> removeById(@PathVariable Long id) {
        sysAuthorityService.removeAuthorityById(id);
        return ok();
    }
}
