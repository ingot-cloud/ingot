package com.ingot.framework.security.account.domain.service;

import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.inbound.DeleteAccountUseCase;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.security.credential.service.PasswordExpirationService;
import com.ingot.framework.security.credential.service.PasswordHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>删除账号用例实现</p>
 *
 * <p>清理顺序：凭证附属数据 → 锁定状态 → 账号主记录（软删）→ 发布删除事件</p>
 * <p>安全事件（{@code account_security_event}）属于审计轨迹，默认不清理；
 * 如有合规需要可在业务侧覆盖 {@link SecurityEventPort#deleteByUser}</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteAccountUseCaseService implements DeleteAccountUseCase {

    private final UserAccountPort userAccountPort;
    private final LockStatePort lockStatePort;
    private final SecurityEventPort securityEventPort;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordExpirationService passwordExpirationService;

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountCommand command) {
        Long userId = command.getUserId();
        var userType = command.getUserType();

        // 1. 验证账号存在
        UserAccount account = userAccountPort.findById(userId, userType)
                .orElseThrow(() -> new BizException("account.not.found", "账号不存在"));

        log.info("[DeleteAccount] 开始删除账号 userId={} userType={} operator={}",
                userId, userType, command.getOperatorId());

        // 2. 清理密码历史（联邦式数据，与账号主库同库）
        passwordHistoryService.deleteByUserId(userId);

        // 3. 清理密码过期记录
        passwordExpirationService.deleteByUserId(userId);

        // 4. 清理锁定状态（account_lock_state）
        lockStatePort.deleteByUser(userId, userType);

        // 5. 软删除账号主记录
        userAccountPort.delete(userId, userType);

        // 6. 发布删除事件（审计用途，不影响主流程，记录在 account_security_event）
        securityEventPort.publishEvent(
                AccountSecurityEvent.accountDeleted(
                        userId, userType,
                        command.getSource(),
                        command.getOperatorId(),
                        command.getOperatorName()));

        log.info("[DeleteAccount] 账号删除完成 userId={} userType={}", userId, userType);
    }
}
