package com.ingot.cloud.pms.web.v1.app;

import com.ingot.cloud.pms.api.model.domain.Member;
import com.ingot.cloud.pms.service.domain.MemberService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.utils.DateUtil;
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
@Tag(description = "AppAuth", name = "app用户信息模块")
@RequestMapping("/v1/app/user")
@RequiredArgsConstructor
public class AppUserAPI implements RShortcuts {
    private final MemberService memberService;

    @Operation(summary = "获取当前用户信息", description = "获取当前用户信息")
    @GetMapping
    public R<?> user() {
        return ok(memberService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @Operation(summary = "更新当前用户信息", description = "更新当前用户信息")
    @PutMapping
    public R<?> updateUser(@RequestBody Member params) {
        params.setId(SecurityAuthContext.getUser().getId());
        params.setPassword(null);
        params.setInitPwd(null);
        params.setStatus(null);
        params.setUpdatedAt(DateUtil.now());
        memberService.updateById(params);
        return ok();
    }
}
