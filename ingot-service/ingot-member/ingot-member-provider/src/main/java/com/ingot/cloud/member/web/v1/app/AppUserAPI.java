package com.ingot.cloud.member.web.v1.app;

import com.ingot.cloud.member.api.model.dto.user.MemberUserDTO;
import com.ingot.cloud.member.service.biz.BizAuthService;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(description = "AppMemberUser", name = "App用户信息模块")
@RequestMapping("/v1/app/user")
@RequiredArgsConstructor
public class AppUserAPI implements RShortcuts {
    private final BizAuthService bizAuthService;
    private final BizUserService bizUserService;

    @Operation(summary = "获取当前用户信息", description = "获取当前用户信息")
    @GetMapping
    public R<?> user() {
        return ok(bizAuthService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @Operation(summary = "更新当前用户信息", description = "更新当前用户信息")
    @PutMapping
    public R<?> updateUser(@RequestBody MemberUserDTO params) {
        params.setId(SecurityAuthContext.getUser().getId());
        params.setStatus(null);
        bizUserService.updateUser(params);
        return ok();
    }
}
