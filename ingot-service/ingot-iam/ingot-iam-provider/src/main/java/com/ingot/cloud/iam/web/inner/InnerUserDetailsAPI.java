package com.ingot.cloud.iam.web.inner;

import java.util.List;
import java.util.Objects;

import com.ingot.cloud.iam.api.model.dto.user.InnerUserDTO;
import com.ingot.cloud.iam.identity.AccountCredentialRepository;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.security.core.identity.UserIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>内部账号查询只读取 {@code iam_account}，不回退旧用户表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/inner/user")
@RequiredArgsConstructor
public class InnerUserDetailsAPI implements RShortcuts {
    private final UserIdentityService userIdentityService;
    private final AccountCredentialRepository accounts;

    /**
     * 加载登录所需账号安全状态与单一成员身份。
     *
     * @param params 登录条件
     * @return 认证资料
     */
    @PostMapping("/details")
    public R<UserDetailsResponse> getUserAuthDetail(@RequestBody UserDetailsRequest params) {
        return ok(userIdentityService.loadUser(params));
    }

    /**
     * 按账号 ID 读取内部账号视图。
     *
     * @param id 账号 ID
     * @return 内部账号视图，不存在时为空
     */
    @GetMapping("/{id}")
    public R<InnerUserDTO> getUserInfo(@PathVariable Long id) {
        return ok(innerUser(id));
    }

    /**
     * 批量读取内部账号视图。
     *
     * @param ids 账号 ID 集合
     * @return 命中的账号视图
     */
    @PostMapping("/list")
    public R<List<InnerUserDTO>> getAllUserInfo(@RequestBody List<Long> ids) {
        return ok(ids.stream().map(this::innerUser).filter(Objects::nonNull).toList());
    }

    private InnerUserDTO innerUser(Long id) {
        return accounts.findById(id)
                .map(account -> accounts.toInnerUser(account, accounts.locked(id)))
                .orElse(null);
    }
}
