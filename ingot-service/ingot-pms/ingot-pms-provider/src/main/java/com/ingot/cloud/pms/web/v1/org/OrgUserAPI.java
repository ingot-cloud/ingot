package com.ingot.cloud.pms.web.v1.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.biz.UserOpsChecker;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : OrgUserAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/22.</p>
 * <p>Time         : 1:55 PM.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/org/user")
@RequiredArgsConstructor
public class OrgUserAPI implements RShortcuts {
    private final SysUserService sysUserService;
    private final BizUserService bizUserService;
    private final UserOpsChecker userOpsChecker;

    @GetMapping
    public R<?> user() {
        return ok(sysUserService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('constants.member.w', 'constants.member.r')")
    @GetMapping("/page")
    public R<?> page(Page<SysUser> page, UserQueryDTO condition) {
        return ok(sysUserService.conditionPage(page, condition));
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('constants.member.w')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody UserDTO params) {
        bizUserService.orgCreateUser(params);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('constants.member.w')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody UserDTO params) {
        bizUserService.orgUpdateUser(params);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('constants.member.w')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizUserService.orgDeleteUser(id);
        return ok();
    }

    @PreAuthorize("@ingot.adminOrHasAnyAuthority('constants.member.r')")
    @GetMapping("/profile/{id}")
    public R<?> userProfile(@PathVariable Long id) {
        return ok(bizUserService.getOrgUserProfile(id));
    }

    @PutMapping("/edit")
    public R<?> updateUserBaseInfo(@RequestBody UserBaseInfoDTO params) {
        long userId = SecurityAuthContext.getUser().getId();
        bizUserService.updateUserBaseInfo(userId, params);
        return ok();
    }
}
