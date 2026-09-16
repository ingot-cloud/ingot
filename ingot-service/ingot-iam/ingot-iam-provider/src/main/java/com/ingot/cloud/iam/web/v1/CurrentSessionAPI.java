package com.ingot.cloud.iam.web.v1;

import com.ingot.cloud.iam.account.CurrentAccountService;
import com.ingot.cloud.iam.session.SessionService;
import com.ingot.framework.commons.model.iam.AccountSelfProfile;
import com.ingot.framework.commons.model.iam.AccountSelfProfileInput;
import com.ingot.framework.commons.model.iam.Bootstrap;
import com.ingot.framework.commons.model.iam.CurrentCapabilities;
import com.ingot.framework.commons.model.iam.CurrentPasswordInput;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.crypto.annotation.InCryptoHybridContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>返回当前认证身份的一致授权视图、本人资料与改密入口。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 当前身份")
@RequestMapping("/v1/me")
@RequiredArgsConstructor
public class CurrentSessionAPI implements RShortcuts {
    private final SessionService sessions;
    private final CurrentAccountService accounts;

    /**
     * 返回当前身份一致授权视图。
     *
     * @return bootstrap
     */
    @Operation(summary = "当前身份一致授权视图")
    @GetMapping("/bootstrap")
    public R<Bootstrap> bootstrap() {
        return ok(sessions.bootstrap());
    }

    /**
     * 返回当前身份操作能力。
     *
     * @return 精确操作集合
     */
    @Operation(summary = "当前身份操作能力")
    @GetMapping("/capabilities")
    public R<CurrentCapabilities> capabilities() {
        return ok(sessions.capabilities());
    }

    /**
     * 返回当前认证账号联系资料。
     *
     * @return 本人资料
     */
    @Operation(summary = "当前账号资料")
    @GetMapping("/profile")
    public R<AccountSelfProfile> profile() {
        return ok(accounts.profile());
    }

    /**
     * 更新当前认证账号联系资料。
     *
     * @param input 联系资料
     * @return 更新后的本人资料
     */
    @Operation(summary = "更新当前账号资料")
    @PatchMapping("/profile")
    public R<AccountSelfProfile> updateProfile(@Valid @RequestBody AccountSelfProfileInput input) {
        return ok(accounts.updateProfile(input));
    }

    /**
     * 修改当前认证账号密码。
     *
     * @param input 新旧密码
     * @return 空成功
     */
    @Operation(summary = "当前账号改密")
    @PutMapping("/password")
    @InCryptoHybridContext
    public R<Void> updatePassword(@Valid @RequestBody CurrentPasswordInput input) {
        accounts.updatePassword(input);
        return ok();
    }
}
