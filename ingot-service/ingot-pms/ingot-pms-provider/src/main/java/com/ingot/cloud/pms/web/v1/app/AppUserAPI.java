package com.ingot.cloud.pms.web.v1.app;

import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.service.domain.AppUserService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : AppUserAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/19.</p>
 * <p>Time         : 16:16.</p>
 */
@RestController
@Tag(description = "appAuth", name = "app用户信息模块")
@RequestMapping("/v1/app/user")
@RequiredArgsConstructor
public class AppUserAPI implements RShortcuts {
    private final AppUserService appUserService;

    @GetMapping
    public R<?> user() {
        return ok(appUserService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @PutMapping
    public R<?> updateUser(@RequestBody AppUser params) {
        params.setId(SecurityAuthContext.getUser().getId());
        params.setPassword(null);
        params.setInitPwd(null);
        params.setStatus(null);
        params.setUpdatedAt(DateUtils.now());
        appUserService.updateById(params);
        return ok();
    }
}
