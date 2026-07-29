package com.ingot.framework.security.account.domain.port.outbound;

import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.commons.model.security.UserTypeEnum;

import java.util.List;

/**
 * 安全事件发布端口
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface SecurityEventPort {

    /**
     * 发布账号安全事件
     *
     * @param event 安全事件
     */
    void publishEvent(AccountSecurityEvent event);

    /**
     * 批量发布事件
     *
     * @param events 事件列表
     */
    void publishBatch(List<AccountSecurityEvent> events);

    /**
     * 删除用户的所有安全事件记录（可选扩展点，默认 NoOp）
     * <p>
     * 安全事件属于合规审计数据，删除账号时<b>默认不清理</b>。
     * 若有 GDPR 等数据清除需求，可在业务侧覆盖此方法执行物理删除。
     * </p>
     *
     * @param userId   用户ID
     * @param userType 用户类型
     */
    default void deleteByUser(Long userId, UserTypeEnum userType) {
        // NoOp：安全事件作为审计轨迹默认保留
    }
}
