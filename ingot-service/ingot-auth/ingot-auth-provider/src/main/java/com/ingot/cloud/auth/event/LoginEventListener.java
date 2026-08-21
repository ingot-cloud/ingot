package com.ingot.cloud.auth.event;

import com.ingot.cloud.member.api.rpc.RemoteMemberLoginRecordService;
import com.ingot.cloud.pms.api.rpc.RemotePmsLoginRecordService;
import com.ingot.framework.commons.model.common.AuthFailureDTO;
import com.ingot.framework.commons.model.common.AuthSuccessDTO;
import com.ingot.framework.commons.model.event.LoginSuccessEvent;
import com.ingot.framework.commons.model.event.LoginFailureEvent;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 登录事件监听器。
 * <p>
 * 异步处理登录成功/失败事件，按用户类型分发到对应服务更新登录状态：
 * <ul>
 *   <li>ADMIN（B 端）→ PMS：{@link RemotePmsLoginRecordService}</li>
 *   <li>APP（C 端）→ Member：{@link RemoteMemberLoginRecordService}</li>
 * </ul>
 * 成功更新 last_login_at/ip 并重置失败计数；失败累加失败计数并触发自动锁定策略。
 * </p>
 *
 * @author wangchao
 * @since 2023/6/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginEventListener {

    private final RemotePmsLoginRecordService pmsLoginRecordService;
    private final RemoteMemberLoginRecordService memberLoginRecordService;

    @Async
    @Order
    @EventListener(LoginSuccessEvent.class)
    public void onLoginSuccess(LoginSuccessEvent event) {
        AuthSuccessDTO payload = event.payload();
        log.info("[LoginEventListener] 登录成功 username={} userId={} userType={} ip={}",
                payload.getUsername(), payload.getUserId(), payload.getUserType(), payload.getIp());

        String userType = payload.getUserType();
        try {
            if (UserTypeEnum.ADMIN.getValue().equals(userType)) {
                com.ingot.cloud.pms.api.model.dto.auth.LoginRecordDTO dto =
                        new com.ingot.cloud.pms.api.model.dto.auth.LoginRecordDTO();
                dto.setSuccess(true);
                dto.setUserId(payload.getUserId());
                dto.setUsername(payload.getUsername());
                dto.setClientIp(payload.getIp());
                dto.setUserType(userType);
                dto.setLoginAt(payload.getTime());
                pmsLoginRecordService.record(dto);
            } else if (UserTypeEnum.APP.getValue().equals(userType)) {
                com.ingot.cloud.member.api.model.dto.auth.LoginRecordDTO dto =
                        new com.ingot.cloud.member.api.model.dto.auth.LoginRecordDTO();
                dto.setSuccess(true);
                dto.setUserId(payload.getUserId());
                dto.setUsername(payload.getUsername());
                dto.setClientIp(payload.getIp());
                dto.setUserType(userType);
                dto.setLoginAt(payload.getTime());
                memberLoginRecordService.record(dto);
            } else {
                log.debug("[LoginEventListener] 未识别用户类型，跳过 userType={}", userType);
            }
        } catch (Exception e) {
            log.error("[LoginEventListener] 通知下游登录成功失败 username={} userType={}",
                    payload.getUsername(), userType, e);
        }
    }

    @Async
    @Order
    @EventListener(LoginFailureEvent.class)
    public void onLoginFailure(LoginFailureEvent event) {
        AuthFailureDTO payload = event.payload();
        log.warn("[LoginEventListener] 登录失败 username={} userType={} ip={} reason={}",
                payload.getUsername(), payload.getUserType(), payload.getIp(), payload.getErrorCode());

        String userType = payload.getUserType();
        Long tenantId = parseTenantId(payload.getTenantId());
        try {
            if (UserTypeEnum.ADMIN.getValue().equals(userType)) {
                com.ingot.cloud.pms.api.model.dto.auth.LoginRecordDTO dto =
                        new com.ingot.cloud.pms.api.model.dto.auth.LoginRecordDTO();
                dto.setSuccess(false);
                dto.setUsername(payload.getUsername());
                dto.setClientIp(payload.getIp());
                dto.setUserType(userType);
                dto.setLoginAt(payload.getTime());
                dto.setFailureReason(payload.getErrorCode());
                dto.setTenantId(tenantId);
                pmsLoginRecordService.record(dto);
            } else if (UserTypeEnum.APP.getValue().equals(userType)) {
                com.ingot.cloud.member.api.model.dto.auth.LoginRecordDTO dto =
                        new com.ingot.cloud.member.api.model.dto.auth.LoginRecordDTO();
                dto.setSuccess(false);
                dto.setUsername(payload.getUsername());
                dto.setClientIp(payload.getIp());
                dto.setUserType(userType);
                dto.setLoginAt(payload.getTime());
                dto.setFailureReason(payload.getErrorCode());
                dto.setTenantId(tenantId);
                memberLoginRecordService.record(dto);
            } else {
                log.debug("[LoginEventListener] 未识别用户类型，跳过 userType={}", userType);
            }
        } catch (Exception e) {
            log.error("[LoginEventListener] 通知下游登录失败失败 username={} userType={}",
                    payload.getUsername(), userType, e);
        }
    }

    private Long parseTenantId(String tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
