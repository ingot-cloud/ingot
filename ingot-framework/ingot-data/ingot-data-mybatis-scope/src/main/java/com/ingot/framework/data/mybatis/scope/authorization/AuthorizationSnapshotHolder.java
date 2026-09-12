package com.ingot.framework.data.mybatis.scope.authorization;

import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;

/**
 * <p>请求期内的授权快照上下文，使用普通 {@link ThreadLocal}，异步不得继承。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AuthorizationSnapshotHolder {

    private static final ThreadLocal<AuthorizationSnapshotDTO> HOLDER = new ThreadLocal<>();

    private AuthorizationSnapshotHolder() {
    }

    /**
     * 绑定当前请求的授权快照。
     *
     * @param snapshot 快照，可为 {@code null} 表示清除
     */
    public static void set(AuthorizationSnapshotDTO snapshot) {
        if (snapshot == null) {
            HOLDER.remove();
            return;
        }
        HOLDER.set(snapshot);
    }

    /**
     * 读取当前请求已绑定的授权快照。
     *
     * @return 快照；未绑定时返回 {@code null}
     */
    public static AuthorizationSnapshotDTO get() {
        return HOLDER.get();
    }

    /**
     * 清除当前线程绑定。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
