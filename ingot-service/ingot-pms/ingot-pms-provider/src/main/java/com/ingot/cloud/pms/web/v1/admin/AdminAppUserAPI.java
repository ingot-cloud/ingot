package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.service.biz.BizAppUserService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : AdminAppUserAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/23.</p>
 * <p>Time         : 10:09.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/admin/appUser")
@RequiredArgsConstructor
public class AdminAppUserAPI implements RShortcuts {
    private final BizAppUserService bizAppUserService;

    @PreAuthorize("@ingot.hasAnyAuthority('app.user')")
    @GetMapping("/page")
    public R<?> userPage(Page<AppUser> page, AppUser filter) {
        return ok(bizAppUserService.page(page, filter));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('app.user')")
    @PutMapping
    public R<?> update(@RequestBody AppUser params) {
        params.setPassword(null);
        params.setInitPwd(null);
        bizAppUserService.updateUser(params);
        return ok();
    }
}
