package com.ingot.framework.security.oauth2.server.authorization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.ingot.framework.security.core.userdetails.InUser;

/**
 * <p>在线会话存储，按 sid 组织会话主数据及其查询索引。</p>
 *
 * <p>本接口只负责会话数据的读写。撤销 OAuth2 Authorization、发布安全事件、执行并发策略
 * 都不在此层 —— 完整撤销由 Auth 侧的会话撤销领域服务编排，本接口的 {@link #removeBySid}
 * 只是其中一步。</p>
 *
 * @author wangchao
 * @since 1.0.0
 * @apiNote 资源服务器只应调用只读方法；写入方仅 Auth 签发链与撤销链。
 * @see OnlineToken
 */
public interface OnlineTokenService {

    /**
     * 落库会话（登录新建或 refresh 原地更新）。
     *
     * <p>同一 sid 重复调用为 upsert：保留原始创建时间与登录环境，更新当前 jti、过期时间与
     * 最近活动时间。</p>
     *
     * @param user         当前登录用户，权限与部门须已切片到目标租户
     * @param registration 会话标识与时效参数
     */
    void save(InUser user, OnlineSessionRegistration registration);

    /**
     * 按会话 ID 读取会话主数据。
     *
     * @param sid 会话 ID
     * @return 会话；不存在（已过期或已撤销）时为空
     */
    Optional<OnlineToken> getBySid(String sid);

    /**
     * 列出用户在指定 Client 下仍然在线的全部会话 ID。
     *
     * @return 会话 ID 列表；顺序不保证
     */
    List<String> listSids(Long tenantId, String clientId, Long userId);

    /**
     * 列出用户在当前租户下全部 Client 仍然在线的会话 ID。
     *
     * <p>账号改密、锁定、禁用等联动撤销不区分登录入口，必须覆盖该用户在本租户的所有 Client。</p>
     *
     * @return 会话 ID 列表；顺序不保证
     * @implNote Client 列表由在线用户注册表推导，不扫描 key 空间。
     */
    List<String> listSids(Long tenantId, Long userId);

    /**
     * 列出同一 IP 下仍然在线的全部会话 ID。
     */
    List<String> listSidsByIp(Long tenantId, String ip);

    /**
     * 列出用户在指定 Client 下的全部在线会话，按创建时间倒序。
     */
    List<OnlineToken> listUserSessions(Long tenantId, String clientId, Long userId);

    /**
     * 列出用户在当前租户下全部 Client 的在线会话，按创建时间倒序。
     */
    List<OnlineToken> listUserSessions(Long tenantId, Long userId);

    /**
     * 列出若干用户在指定 Client 下的在线会话，按创建时间倒序。
     *
     * <p>管理面按在线用户分页展开会话时使用：一次拉取这些用户的集合与主数据，避免逐用户往返。</p>
     */
    List<OnlineToken> listUserSessions(Long tenantId, String clientId, Collection<Long> userIds);

    /**
     * 判断会话是否在线。
     *
     * @param sid 会话 ID
     * @return {@code true} 表示会话主数据存在
     */
    boolean isOnlineSid(String sid);

    /**
     * 删除会话主数据及其全部索引。
     *
     * @param sid 会话 ID
     */
    void removeBySid(String sid);

    /**
     * 分页获取在线用户 ID，按会话最晚过期时间倒序。
     *
     * @param offset 偏移量
     * @param limit  数量
     */
    List<Long> getOnlineUsers(Long tenantId, String clientId, long offset, long limit);

    /**
     * 获取在线用户总数（不含已过期条目）。
     */
    long getOnlineUserCount(Long tenantId, String clientId);

    /**
     * 清理指定租户 Client 下已过期的在线用户条目。
     *
     * @return 清理条目数
     */
    long cleanExpiredOnlineUsers(Long tenantId, String clientId);

    /**
     * 遍历在线用户注册表清理全部已过期条目，摘除已消亡的注册表成员，并删除对应的用户会话集合。
     *
     * @return 清理条目数
     * @implNote 通过注册表枚举待清理 key，不得使用 {@code KEYS} 扫描生产 Redis。
     */
    long cleanAllExpiredOnlineUsers();
}
