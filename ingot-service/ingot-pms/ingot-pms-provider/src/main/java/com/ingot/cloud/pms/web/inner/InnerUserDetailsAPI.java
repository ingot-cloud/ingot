package com.ingot.cloud.pms.web.inner;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.InnerUserDTO;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.security.core.identity.UserIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : UserDetailApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 4:41 下午.</p>
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/inner/user")
@RequiredArgsConstructor
public class InnerUserDetailsAPI implements RShortcuts {
    private final SysUserService sysUserService;
    private final UserIdentityService userIdentityService;

    @PostMapping("/details")
    public R<UserDetailsResponse> getUserAuthDetail(@RequestBody UserDetailsRequest params) {
        return ok(userIdentityService.loadUser(params));
    }

    @GetMapping("/{id}")
    public R<InnerUserDTO> getUserInfo(@PathVariable Long id) {
        return ok(UserConvert.INSTANCE.toInnerUser(sysUserService.getById(id)));
    }

    @PostMapping("/list")
    public R<List<InnerUserDTO>> getAllUserInfo(@RequestBody List<Long> ids) {
        return ok(sysUserService.list(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, ids))
                .stream().map(UserConvert.INSTANCE::toInnerUser).toList());
    }
}
