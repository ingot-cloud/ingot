package com.ingot.cloud.security.service.session;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ingot.cloud.security.api.model.dto.session.PlatformSessionQueryDTO;
import com.ingot.cloud.security.api.model.dto.session.PlatformUserSessionRevokeDTO;
import com.ingot.cloud.security.api.model.vo.session.PlatformSessionVO;

/**
 * <p>安全中心会话管理面，向管理员提供在线会话查询与强制下线。</p>
 *
 * <p>会话主数据只在 Auth 侧 Redis，本服务不直连该存储，一律经 Auth Inner RPC 读写；
 * 用户名与租户名不在会话事实里，由本层调 PMS 补全，PMS 不可用时降级为空名称，
 * 保证 PMS 故障时管理员仍能按 sid 下线。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote Auth 不可达时方法抛出异常（由 Feign 错误解码为业务异常），不做静默降级 ——
 * 管理面必须让管理员看到「查不到/下线失败」，而不是返回空列表让人误判已无会话。
 */
public interface SessionAdminService {

    /**
     * 分页查询在线会话，一条记录对应一个会话。
     *
     * @param params {@code clientId} 与 {@code userId} 至少给一个；{@code tenantId} 为空取当前租户
     */
    IPage<PlatformSessionVO> page(PlatformSessionQueryDTO params);

    /**
     * 查询会话详情，会话不存在时返回 {@code null}。
     */
    PlatformSessionVO getBySid(String sid);

    /**
     * 强制下线单个会话。
     *
     * @param actorId 操作者用户 ID，写入安全事件
     * @return {@code true} 表示本次调用确实撤销了一个存活会话
     */
    boolean revokeBySid(String sid, Long actorId);

    /**
     * 强制下线用户会话。
     *
     * @param actorId 操作者用户 ID，写入安全事件
     * @return 实际撤销的会话数
     */
    int revokeByUser(PlatformUserSessionRevokeDTO params, Long actorId);
}
