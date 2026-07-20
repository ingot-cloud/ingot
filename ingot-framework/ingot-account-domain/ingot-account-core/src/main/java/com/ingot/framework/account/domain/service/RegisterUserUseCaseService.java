package com.ingot.framework.account.domain.service;

import java.time.LocalDateTime;

import com.ingot.framework.account.domain.config.AccountMessageSource;
import com.ingot.framework.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.account.domain.model.UserAccount;
import com.ingot.framework.account.domain.model.enums.EventSource;
import com.ingot.framework.account.domain.model.enums.SecurityEventType;
import com.ingot.framework.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.account.domain.port.outbound.UserAccountPort;
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
 * 注册用户用例实现
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCaseService implements RegisterUserUseCase {

    private final UserAccountPort userAccountPort;
    private final LockStatePort lockStatePort;
    private final SecurityEventPort securityEventPort;
    private final CredentialSecurityService credentialSecurityService;
    private final PasswordEncoder passwordEncoder;

    private final MessageSourceAccessor message = AccountMessageSource.getAccessor();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccount register(RegisterUserCommand command) {
        boolean isAdminCreate = command.getCreationSource() == CreationSource.ADMIN_CREATE;

        log.info("[RegisterUser] 创建账号 username={} source={}",
                command.getUsername(),
                isAdminCreate ? "ADMIN_CREATE" : "SELF_REGISTER");

        // 1. 校验用户名唯一性
        if (userAccountPort.existsByUsername(command.getUsername(), command.getUserType())) {
            throw new IllegalArgumentException(message.getMessage("Account.UsernameExists"));
        }

        // 2. 凭证策略校验（仅用户自主注册时执行）
        //    ADMIN_CREATE 场景密码为随机值，跳过强度校验；用户首次登录时须自行修改并在改密时触发策略验证
        if (!isAdminCreate) {
            CredentialValidateRequest strengthRequest = CredentialValidateRequest.builder()
                    .scene(CredentialScene.REGISTER)
                    .userId(null)    // 新用户尚无 ID，不触发历史保存和过期更新
                    .username(command.getUsername())
                    .password(command.getPassword())
                    .userType(command.getUserType())
                    .manualProcessError(false)
                    .autoProcessUpdatePasswordLogic(false)
                    .build();
            credentialSecurityService.validate(strengthRequest);
        }

        // 3. 加密密码
        String passwordHash = passwordEncoder.encode(command.getPassword());

        // 4. 构建并保存用户账号
        boolean mustChangePwd = command.getMustChangePwd() == null ? Boolean.TRUE
                : command.getMustChangePwd();

        LocalDateTime now = LocalDateTime.now();
        UserAccount account = UserAccount.builder()
                .userType(command.getUserType())
                .username(command.getUsername())
                .password(passwordHash)
                .nickname(command.getNickname())
                .phone(command.getPhone())
                .email(command.getEmail())
                .avatar(command.getAvatar())
                .mustChangePwd(mustChangePwd)
                .passwordChangedAt(now)
                .enabled(true)
                .locked(false)
                .version(0L)
                .createdAt(now)
                .createdBy(command.getCreatedBy())
                .build();

        UserAccount savedAccount = userAccountPort.save(account);
        Long userId = savedAccount.getId();

        // 5. 初始化锁定状态
        lockStatePort.initialize(userId, command.getUserType());

        // 6. 保存初始密码历史 + 初始化密码过期记录（userId 已确定）
        //    两种场景均需初始化：后续改密时凭证策略会检查历史和过期，确保链路完整
        credentialSecurityService.savePasswordHistory(userId, passwordHash);
        credentialSecurityService.updatePasswordExpiration(userId);
        // 强制改密标记与账号域 mustChangePwd 保持一致（凭证域镜像）
        if (mustChangePwd) {
            credentialSecurityService.markForceChange(userId, true);
        }

        // 7. 发布账号创建事件
        //    事件来源优先使用命令中显式指定的 eventSource；
        //    未指定时，自助注册一律落为 SYSTEM；管理员创建按 userType 派生，避免硬编码到单一业务域。
        EventSource source = resolveEventSource(command, isAdminCreate);
        securityEventPort.publishEvent(AccountSecurityEvent.builder()
                .userId(userId)
                .userType(command.getUserType())
                .eventType(SecurityEventType.ACCOUNT_CREATED)
                .result(true)
                .source(source)
                .operatorId(command.getCreatedBy())
                .createdAt(now)
                .build());

        log.info("[RegisterUser] 账号创建成功 username={} userId={} mustChangePwd={}",
                command.getUsername(), userId, mustChangePwd);
        return savedAccount;
    }

    /**
     * 解析账号创建事件来源：
     * <ol>
     *   <li>命令显式传入的 {@code eventSource} 优先使用</li>
     *   <li>自助注册统一落 {@link EventSource#SYSTEM}</li>
     *   <li>管理员创建按 {@code userType} 派生：{@code ADMIN → PMS}、{@code APP → MEMBER}，
     *       其余未识别类型降级为 {@link EventSource#SYSTEM}</li>
     * </ol>
     */
    private EventSource resolveEventSource(RegisterUserCommand command, boolean isAdminCreate) {
        if (command.getEventSource() != null) {
            return command.getEventSource();
        }
        if (!isAdminCreate) {
            return EventSource.SYSTEM;
        }
        if (command.getUserType() == null) {
            return EventSource.SYSTEM;
        }
        return switch (command.getUserType()) {
            case ADMIN -> EventSource.PMS;
            case APP -> EventSource.MEMBER;
        };
    }
}
