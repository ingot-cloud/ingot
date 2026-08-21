package com.ingot.framework.security.oauth2.server.authorization.session;

/**
 * <p>会话撤销回调，供上层在撤销成功后做审计、安全事件上报等旁路处理。</p>
 *
 * <p>授权服务器只负责撤销本身，不感知安全事件模型，因此事件落库交由本回调的实现完成
 * （如 Auth 服务把撤销转成 {@code security_event}）。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 实现须自行保证快速返回且不抛出异常；调用方会捕获异常并继续，
 * 撤销结果不会因回调失败而回滚。
 */
@FunctionalInterface
public interface SessionRevocationListener {

    /**
     * 会话已被撤销。
     *
     * @param event 撤销前的会话快照与撤销上下文
     */
    void onRevoked(SessionRevokedEvent event);
}
