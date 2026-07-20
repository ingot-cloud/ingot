package com.ingot.framework.account.domain.service;

import java.time.LocalDateTime;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.account.domain.config.AccountMessageSource;
import com.ingot.framework.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.account.domain.model.UserAccount;
import com.ingot.framework.account.domain.model.enums.SecurityEventType;
import com.ingot.framework.account.domain.port.inbound.ChangePasswordUseCase;
import com.ingot.framework.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.account.domain.port.outbound.UserCredentialPort;
import com.ingot.framework.commons.utils.AssertionUtil;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 修改密码用例实现
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordUseCaseService implements ChangePasswordUseCase {

    private final UserAccountPort userAccountPort;
    private final UserCredentialPort userCredentialPort;
    private final SecurityEventPort securityEventPort;
    private final CredentialSecurityService credentialSecurityService;
    private final PasswordEncoder passwordEncoder;

    private final MessageSourceAccessor message = AccountMessageSource.getAccessor();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordCommand command) {
        log.info("用户 {} 请求修改密码", command.getUserId());

        // 1. 校验输入的密码是否一致性
        AssertionUtil.checkArgument(StrUtil.equals(command.getNewPassword(), command.getConfirmPassword()),
                getMessage("Password.TwoNewPwdNotMatch"));

        // 2. 校验用户存在
        UserAccount account = userAccountPort.findById(command.getUserId(), command.getUserType())
                .orElseThrow(() ->
                        new IllegalArgumentException(getMessage("Password.UserNonExist")));

        // 3. 校验旧密码正确性
        String currentPasswordHash = account.getPassword();
        AssertionUtil.checkArgument(passwordEncoder.matches(command.getOldPassword(), currentPasswordHash),
                getMessage("Password.Incorrect"));

        // 4. 通过 credential 服务校验新密码（强度 + 历史）
        //    CHANGE_PASSWORD 场景：校验密码强度、历史，校验通过后自动保存历史并更新过期时间
        CredentialValidateRequest request = CredentialValidateRequest.builder()
                .scene(CredentialScene.CHANGE_PASSWORD)
                .userId(command.getUserId())
                .username(account.getUsername())
                .password(command.getNewPassword())
                .userType(command.getUserType())
                .manualProcessError(false)
                .autoProcessUpdatePasswordLogic(true)
                .build();
        credentialSecurityService.validate(request); // 失败自动抛出异常

        // 5. 更新密码哈希（乐观锁），同时清除强制修改密码标记（用户已主动修改）
        String newPasswordHash = passwordEncoder.encode(command.getNewPassword());
        boolean updated = userCredentialPort.updatePassword(
                command.getUserId(),
                command.getUserType(),
                newPasswordHash,
                LocalDateTime.now(),
                account.getVersion(),
                false
        );

        AssertionUtil.checkArgument(updated, getMessage("Password.UpdateFailed"));

        // 6. 发布密码修改事件
        securityEventPort.publishEvent(AccountSecurityEvent.passwordChanged(
                command.getUserId(), command.getUserType()));

        log.info("用户 {} 密码修改成功", command.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordCommand command) {
        log.info("管理员 {} 重置用户 {} 密码", command.getOperatorId(), command.getUserId());

        // 1. 校验用户存在
        UserAccount account = userAccountPort.findById(command.getUserId(), command.getUserType())
                .orElseThrow(() -> new IllegalArgumentException(getMessage("Password.UserNonExist")));

        // 2. 管理员重置场景：不走密码策略，同时标记强制改密（mustChangePwd=true）
        //    三个字段一次 UPDATE 写入，无需额外 SQL
        String newPasswordHash = passwordEncoder.encode(command.getNewPassword());
        boolean updated = userCredentialPort.updatePassword(
                command.getUserId(),
                command.getUserType(),
                newPasswordHash,
                LocalDateTime.now(),
                account.getVersion(),
                true
        );

        AssertionUtil.checkArgument(updated, getMessage("Password.UpdateFailed"));

        // 2.1 重置过期信息并置位强制改密标记（凭证域与账号域 mustChangePwd 保持一致）
        credentialSecurityService.updatePasswordExpiration(command.getUserId());
        credentialSecurityService.markForceChange(command.getUserId(), true);

        // 3. 发布密码重置事件
        securityEventPort.publishEvent(AccountSecurityEvent.builder()
                .userId(command.getUserId())
                .userType(command.getUserType())
                .eventType(SecurityEventType.PASSWORD_RESET)
                .result(true)
                .source(command.getSource())
                .operatorId(command.getOperatorId())
                .operatorName(command.getOperatorName())
                .createdAt(LocalDateTime.now())
                .build());

        log.info("用户 {} 密码重置成功", command.getUserId());
    }

    @Override
    public void forceChangePassword(ForceChangePasswordCommand command) {
        log.info("用户 {} 强制修改密码", command.getUserId());

        // 1. 校验用户存在
        UserAccount account = userAccountPort.findById(command.getUserId(), command.getUserType())
                .orElseThrow(() -> new IllegalArgumentException(getMessage("Password.UserNonExist")));

        // 2. 密码策略
        CredentialValidateRequest request = CredentialValidateRequest.builder()
                .scene(CredentialScene.CHANGE_PASSWORD)
                .userId(command.getUserId())
                .username(account.getUsername())
                .password(command.getNewPassword())
                .userType(command.getUserType())
                .manualProcessError(false)
                .autoProcessUpdatePasswordLogic(true)
                .build();
        credentialSecurityService.validate(request); // 失败自动抛出异常

        // 3. 更新用户密码
        String newPasswordHash = passwordEncoder.encode(command.getNewPassword());
        boolean updated = userCredentialPort.updatePassword(
                command.getUserId(),
                command.getUserType(),
                newPasswordHash,
                LocalDateTime.now(),
                account.getVersion(),
                false
        );

        AssertionUtil.checkArgument(updated, getMessage("Password.UpdateFailed"));

        // 4. 发布密码重置事件
        securityEventPort.publishEvent(AccountSecurityEvent.builder()
                .userId(command.getUserId())
                .userType(command.getUserType())
                .eventType(SecurityEventType.FORCE_CHANGE_PASSWORD)
                .result(true)
                .source(command.getSource())
                .createdAt(LocalDateTime.now())
                .build());

        log.info("用户 {} 密码强制修改成功", command.getUserId());
    }

    private String getMessage(String code) {
        return message.getMessage(code);
    }
}
