package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.AppUserCreateDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.service.biz.BizAppUserService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.HasAnyAuthority;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : AdminAppUserAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/23.</p>
 * <p>Time         : 10:09.</p>
 */
@Slf4j
@Tag(description = "appUser", name = "App端用户管理模块")
@RestController
@RequestMapping(value = "/v1/admin/appUser")
@RequiredArgsConstructor
public class AdminAppUserAPI implements RShortcuts {
    private final BizAppUserService bizAppUserService;

    @HasAnyAuthority({"app:user"})
    @GetMapping("/page")
    public R<?> userPage(Page<AppUser> page, AppUser filter) {
        return ok(bizAppUserService.page(page, filter));
    }

    @HasAnyAuthority({"app:user:w"})
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody AppUserCreateDTO params) {
        return ok(bizAppUserService.createUser(params));
    }

    @HasAnyAuthority({"app:user:w"})
    @PutMapping
    public R<?> update(@RequestBody AppUser params) {
        params.setPassword(null);
        params.setInitPwd(null);
        bizAppUserService.updateUser(params);
        return ok();
    }

    @HasAnyAuthority({"app:user:w"})
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizAppUserService.deleteUser(id);
        return ok();
    }

    @HasAnyAuthority({"app:user:r"})
    @GetMapping("/orgInfo/{userId}")
    public R<?> orgInfo(@PathVariable Long userId) {
        return ok(bizAppUserService.userOrgInfo(userId));
    }

    @HasAnyAuthority({"app:user:w"})
    @PutMapping("/org")
    public R<?> userOrgEdit(@RequestBody UserOrgEditDTO params) {
        bizAppUserService.userOrgEdit(params);
        return ok();
    }

    @HasAnyAuthority({"app:user:w"})
    @PutMapping("/org/leave")
    public R<?> userOrgLeave(@RequestBody UserOrgEditDTO params) {
        bizAppUserService.userOrgLeave(params);
        return ok();
    }

    @HasAnyAuthority({"app:user:w"})
    @PutMapping("/resetPwd/{userId}")
    public R<?> resetPwd(@PathVariable Long userId) {
        return ok(bizAppUserService.resetPwd(userId));
    }

    @HasAnyAuthority({"app:user:r", "app:user:w"})
    @GetMapping("/profile/{id}")
    public R<?> userProfile(@PathVariable Long id) {
        return ok(bizAppUserService.getUserProfile(id));
    }

    @PutMapping("/edit")
    public R<?> updateUserBaseInfo(@RequestBody UserBaseInfoDTO params) {
        long userId = SecurityAuthContext.getUser().getId();
        bizAppUserService.updateUserBaseInfo(userId, params);
        return ok();
    }

}
