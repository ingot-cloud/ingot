package com.ingot.cloud.iam.web.inner;

import java.util.List;

import com.ingot.cloud.iam.api.model.convert.UserConvert;
import com.ingot.cloud.iam.api.model.dto.user.InnerUserDTO;
import com.ingot.cloud.iam.identity.AccountCredentialRepository;
import com.ingot.cloud.iam.service.domain.SysUserService;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.security.core.identity.UserIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
    private final AccountCredentialRepository accounts;

    @PostMapping("/details")
    public R<UserDetailsResponse> getUserAuthDetail(@RequestBody UserDetailsRequest params) {
        return ok(userIdentityService.loadUser(params));
    }

    @GetMapping("/{id}")
    public R<InnerUserDTO> getUserInfo(@PathVariable Long id) {
        return ok(innerUser(id));
    }

    @PostMapping("/list")
    public R<List<InnerUserDTO>> getAllUserInfo(@RequestBody List<Long> ids) {
        return ok(ids.stream().map(this::innerUser).filter(java.util.Objects::nonNull).toList());
    }

    private InnerUserDTO innerUser(Long id) {
        try {
            var modern = accounts.findById(id);
            if (modern.isPresent()) {
                return accounts.toInnerUser(modern.get());
            }
        } catch (DataAccessException exception) {
            log.debug("新模型账号不可用，回退旧用户表 id={}", id, exception);
        }
        var user = sysUserService.getById(id);
        return user == null ? null : UserConvert.INSTANCE.toInnerUser(user);
    }
}
