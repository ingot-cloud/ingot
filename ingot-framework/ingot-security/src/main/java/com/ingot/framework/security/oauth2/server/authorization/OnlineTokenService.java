package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.ingot.framework.security.core.userdetails.InUser;

/**
 * 在线Token服务
 * 职责：
 * 1. 管理当前在线的Token信息
 * 2. 支持唯一登录验证
 * 3. 支持强制下线
 * 4. Token信息查询
 *
 * <p>Author: wangchao</p>
 * <p>Date: 2024/12/17</p>
 */
public interface OnlineTokenService {

    /**
     * 保存在线 Token 信息
     *
     * @param user      用户信息
     * @param jti       JTI
     * @param expiresAt 过期时间
     */
    void save(InUser user, String jti, Instant expiresAt);

    /**
     * 根据用户信息获取当前在线Token
     * 用于唯一登录验证
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     * @return Token信息
     */
    Optional<OnlineToken> getByUser(Long userId, Long tenantId, String clientId);

    /**
     * 根据JTI获取Token信息
     * 用于资源服务器验证Token时获取扩展信息
     *
     * @param jti JWT ID
     * @return Token信息
     */
    Optional<OnlineToken> getByJti(String jti);

    /**
     * 删除指定用户的在线Token（强制下线）
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     */
    void removeByUser(Long userId, Long tenantId, String clientId);

    /**
     * 删除指定JTI的Token
     *
     * @param jti JWT ID
     */
    void removeByJti(String jti);

    /**
     * 检查Token是否在线（未被强制下线）
     *
     * @param jti JWT ID
     * @return true-在线，false-已下线
     */
    boolean isOnline(String jti);

    /**
     * 获取在线用户列表（分页）
     *
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     * @param offset   偏移量
     * @param limit    数量
     * @return 用户ID列表
     */
    List<Long> getOnlineUsers(Long tenantId, String clientId, long offset, long limit);

    /**
     * 获取在线用户总数
     *
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     * @return 在线用户数
     */
    long getOnlineUserCount(Long tenantId, String clientId);


    List<OnlineToken> getUserAllTokens(Long userId, Long tenantId, String clientId);

    /**
     * 清理过期的在线用户（定时任务调用）
     *
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     * @return 清理的用户数
     */
    long cleanExpiredOnlineUsers(Long tenantId, String clientId);

    /**
     * 清理所有租户的过期在线用户（定时任务调用）
     *
     * @return 清理的总用户数
     */
    long cleanAllExpiredOnlineUsers();
}
