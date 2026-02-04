package com.ingot.framework.security.credential.service;

import java.util.List;

import com.ingot.framework.security.credential.model.domain.PasswordHistory;

/**
 * 密码历史服务接口（通用）
 * <p>各业务服务需要实现此接口来管理自己的密码历史</p>
 *
 * @author jymot
 * @since 2026-01-23
 */
public interface PasswordHistoryService {

    /**
     * 获取用户的密码历史记录
     *
     * @param userId 用户ID
     * @param limit  获取数量（最近N条）
     * @return 密码历史列表（按创建时间倒序）
     */
    List<PasswordHistory> getRecentHistory(Long userId, int limit);

    /**
     * 保存密码历史（环形缓冲）
     * <p>当达到最大记录数时，会覆盖最旧的记录</p>
     *
     * @param userId     用户ID
     * @param password   密码原始值
     * @param maxRecords 最大保留记录数
     */
    void saveHistory(Long userId, String password, int maxRecords);

    /**
     * 检查密码是否在历史中使用过
     *
     * @param userId     用户ID
     * @param password   密码
     * @param checkCount 检查最近N条记录
     * @return true-已使用过，false-未使用过
     */
    boolean isPasswordUsed(Long userId, String password, int checkCount);

    /**
     * 删除用户的所有密码历史
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}
