package com.ingot.cloud.pms.web.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.biz.UserOpsChecker;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * <p>Description  : UserApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/11.</p>
 * <p>Time         : 6:48 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/user")
@RequiredArgsConstructor
public class UserAPI implements RShortcuts {
    private final SysUserService sysUserService;
    private final BizUserService bizUserService;
    private final UserOpsChecker userOpsChecker;

    @GetMapping
    public R<?> user() {
        return ok(sysUserService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.read', 'basic.user.write')")
    @GetMapping("/page")
    public R<?> page(Page<SysUser> page, UserDTO condition) {
        return ok(sysUserService.conditionPage(page, condition));
    }

    @PreAuthorize("@ingot.hasAuthority('basic.user.write')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody UserDTO params) {
        params.setInitPwd(null);
        sysUserService.createUser(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAuthority('basic.user.write')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody UserDTO params) {
        if (params.getStatus() == UserStatusEnum.LOCK) {
            userOpsChecker.disableUser(params.getId());
        }
        sysUserService.updateUser(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAuthority('basic.user.write')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        userOpsChecker.removeUser(id);
        sysUserService.removeUserById(id);
        return ok();
    }

    @GetMapping("/profile/{id}")
    public R<?> userProfile(@PathVariable Long id) {
        return ok(bizUserService.getUserProfile(id));
    }

    @PutMapping("/edit")
    public R<?> updateUserBaseInfo(@RequestBody UserBaseInfoDTO params) {
        long userId = SecurityAuthContext.getUser().getId();
        bizUserService.updateUserBaseInfo(userId, params);
        return ok();
    }


}
