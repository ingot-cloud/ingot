package com.ingot.framework.security.account.domain.model;

import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.model.enums.SecurityEventType;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号安全事件领域模型
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
@Builder
public class AccountSecurityEvent {
    /**
     * 事件ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型
     */
    private UserTypeEnum userType;

    /**
     * 事件类型
     */
    private SecurityEventType eventType;

    /**
     * 事件分类（从 eventType 派生）
     */
    public String getEventCategory() {
        return eventType != null ? eventType.getCategory() : null;
    }

    /**
     * 原因代码
     */
    private String reasonCode;

    /**
     * 详细描述
     */
    private String reasonDetail;

    /**
     * 操作结果（null-不适用；true-成功；false-失败）
     * <p>
     * 状态变更类事件（锁定/解锁/启用/禁用）无需结果，保持 null；
     * 认证类和凭证类事件有明确的成功/失败语义。
     * </p>
     */
    private Boolean result;

    /**
     * 事件来源（{@link EventSource}）
     */
    private EventSource source;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 客户端信息（User-Agent）
     */
    private String userAgent;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 扩展数据（JSON格式）
     */
    private String extraData;

    /**
     * 事件时间
     */
    private LocalDateTime createdAt;

    /**
     * 当前事件是否为成功结果
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(result);
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建登录成功事件
     */
    public static AccountSecurityEvent loginSuccess(Long userId, UserTypeEnum userType,
                                                    String username, String clientIp,
                                                    String userAgent, Long tenantId) {
        return AccountSecurityEvent.builder()
                .userId(userId)
                .userType(userType)
                .eventType(SecurityEventType.LOGIN_SUCCESS)
                .result(true)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .tenantId(tenantId)
                .source(EventSource.AUTH)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建登录失败事件
     */
    public static AccountSecurityEvent loginFailure(Long userId, UserTypeEnum userType,
                                                    String username, String clientIp,
                                                    String failureReason) {
        return AccountSecurityEvent.builder()
                .userId(userId)
                .userType(userType)
                .eventType(SecurityEventType.LOGIN_FAILURE)
                .result(false)
                .reasonDetail(failureReason)
                .clientIp(clientIp)
                .source(EventSource.AUTH)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建账号锁定事件
     *
     * @param source 来源（手动锁定传 {@link EventSource#PMS}，自动锁定传 {@link EventSource#SYSTEM}）
     */
    public static AccountSecurityEvent accountLocked(Long userId, UserTypeEnum userType,
                                                     String reasonCode, String reasonDetail,
                                                     EventSource source, Long operatorId,
                                                     String operatorName) {
        return AccountSecurityEvent.builder()
                .userId(userId)
                .userType(userType)
                .eventType(SecurityEventType.ACCOUNT_LOCKED)
                .reasonCode(reasonCode)
                .reasonDetail(reasonDetail)
                .source(source)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建账号解锁事件
     *
     * @param source 来源（手动解锁传 {@link EventSource#PMS} 等，自动解锁传 {@link EventSource#SYSTEM}）
     */
    public static AccountSecurityEvent accountUnlocked(Long userId, UserTypeEnum userType,
                                                       String reason, EventSource source,
                                                       Long operatorId, String operatorName) {
        return AccountSecurityEvent.builder()
                .userId(userId)
                .userType(userType)
                .eventType(SecurityEventType.ACCOUNT_UNLOCKED)
                .reasonDetail(reason)
                .source(source)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建账号删除事件
     *
     * @param source 来源（管理端传 {@link EventSource#PMS}，自助注销传对应来源）
     */
    public static AccountSecurityEvent accountDeleted(Long userId, UserTypeEnum userType,
                                                      EventSource source, Long operatorId,
                                                      String operatorName) {
        return AccountSecurityEvent.builder()
                .userId(userId)
                .userType(userType)
                .eventType(SecurityEventType.ACCOUNT_DELETED)
                .source(source)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建密码修改事件
     */
    public static AccountSecurityEvent passwordChanged(Long userId, UserTypeEnum userType) {
        return AccountSecurityEvent.builder()
                .userId(userId)
                .userType(userType)
                .eventType(SecurityEventType.PASSWORD_CHANGED)
                .result(true)
                .source(EventSource.SYSTEM)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
