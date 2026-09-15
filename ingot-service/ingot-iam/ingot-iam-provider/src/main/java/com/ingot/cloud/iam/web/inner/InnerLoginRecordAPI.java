package com.ingot.cloud.iam.web.inner;

import com.ingot.cloud.iam.api.model.dto.auth.LoginRecordDTO;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.inbound.RecordLoginUseCase;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部登录记录接口（供 Auth 服务回调，异步处理失败计数、锁定、最后登录时间更新）
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/inner/user/login")
@Permit(mode = PermitMode.INNER)
public class InnerLoginRecordAPI implements RShortcuts {

    private final RecordLoginUseCase recordLoginUseCase;
    private final UserAccountPort userAccountPort;

    @PostMapping("/record")
    public R<Void> record(@RequestBody LoginRecordDTO dto) {
        Long userId = dto.getUserId();

        // 如果 Auth 没有传 userId（如密码错误但用户存在），尝试按用户名查找
        if (userId == null && dto.getUsername() != null) {
            userId = userAccountPort.findByUsername(dto.getUsername(), UserTypeEnum.ADMIN)
                    .map(UserAccount::getId)
                    .orElse(null);
        }

        if (userId == null) {
            // 用户不存在，无需记录（防止枚举攻击，仅静默忽略）
            log.debug("[InnerLoginRecordAPI] 未找到用户 username={}, 忽略登录记录", dto.getUsername());
            return ok();
        }

        UserTypeEnum userType = UserTypeEnum.ADMIN;

        RecordLoginUseCase.LoginCommand command = RecordLoginUseCase.LoginCommand.builder()
                .userId(userId)
                .userType(userType)
                .username(dto.getUsername())
                .clientIp(dto.getClientIp())
                .tenantId(dto.getTenantId())
                .failureReason(dto.getFailureReason())
                .build();

        if (dto.isSuccess()) {
            recordLoginUseCase.recordSuccess(command);
        } else {
            recordLoginUseCase.recordFailure(command);
        }

        return ok();
    }
}
