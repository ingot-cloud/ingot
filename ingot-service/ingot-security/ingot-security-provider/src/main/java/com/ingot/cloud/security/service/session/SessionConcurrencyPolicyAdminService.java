package com.ingot.cloud.security.service.session;

import java.util.List;

import com.ingot.cloud.security.model.domain.SessionConcurrencyPolicy;

/**
 * <p>并发会话策略管理面服务，负责策略 CRUD 与变更后的失效广播。</p>
 *
 * <p>本服务只维护策略数据，不感知会话执行：策略如何匹配登录、超限如何处置都发生在 Auth 侧。
 * 每次写操作提交后发布 {@code SecurityPolicyChangedSpringEvent}，由通用发布器转成跨节点失效事件，
 * 使 Auth 各实例在下一次登录前拿到新策略。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote {@code GLOBAL} 兜底策略不可删除，删除请求会被拒绝；把它调成
 *          {@code maxSessions=0} 即等价于关闭并发限制。
 */
public interface SessionConcurrencyPolicyAdminService {

    /**
     * 查询全部策略，按 scope、clientId、userType 升序。
     */
    List<SessionConcurrencyPolicy> list();

    /**
     * 按 ID 查询策略。
     *
     * @return 策略；不存在时抛业务异常
     */
    SessionConcurrencyPolicy getById(Long id);

    /**
     * 新增策略。
     *
     * @return 已落库的策略（含生成的 ID）
     */
    SessionConcurrencyPolicy create(SessionConcurrencyPolicy policy);

    /**
     * 按 ID 更新策略；scope 与其定位字段允许调整，但不得与既有策略冲突。
     */
    SessionConcurrencyPolicy update(SessionConcurrencyPolicy policy);

    /**
     * 删除策略。
     *
     * @param id 策略 ID；{@code GLOBAL} 策略不可删除
     */
    void delete(Long id);
}
