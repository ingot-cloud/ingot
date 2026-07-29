package com.ingot.framework.security.account.adapter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ingot.framework.security.account.adapter.entity.AccountLockStateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 锁定状态 Mapper
 *
 * @author jymot
 * @since 2026-02-13
 */
@Mapper
public interface AccountLockStateMapper extends BaseMapper<AccountLockStateEntity> {

    /**
     * 原子 upsert：若记录不存在则插入（失败次数=1），否则原子递增失败次数。
     * <p>
     * 利用 {@code ON DUPLICATE KEY UPDATE} 保证并发安全，消除 select-then-update 的竞态窗口。
     * 依赖唯一键 {@code uk_user_id_type (user_id, user_type)}。
     * </p>
     *
     * @param userId   用户ID
     * @param userType 用户类型值（{@code UserTypeEnum.value}）
     * @param now      当前时间
     */
    void upsertIncrementFailCount(@Param("userId") Long userId,
                                  @Param("userType") String userType,
                                  @Param("now") LocalDateTime now);

    /**
     * 直接将失败次数清零（不做 SELECT，直接 UPDATE），适用于登录成功后重置。
     *
     * @param userId   用户ID
     * @param userType 用户类型值
     * @param now      当前时间
     */
    void resetFailCountDirect(@Param("userId") Long userId,
                              @Param("userType") String userType,
                              @Param("now") LocalDateTime now);

    /**
     * 游标分页查找过期的临时锁定
     * <p>
     * 使用 {@code id > afterId} 游标，按 id 升序，避免大数据量下 OFFSET 效率退化。
     * </p>
     *
     * @param now      当前时间，locked_until &lt;= now 的记录视为过期
     * @param afterId  游标：仅返回 id &gt; afterId 的记录
     * @param pageSize 每页最大记录数
     * @return 当前游标之后的一批过期锁定记录
     */
    List<AccountLockStateEntity> findExpiredLocksByPage(@Param("now") LocalDateTime now,
                                                        @Param("afterId") Long afterId,
                                                        @Param("pageSize") int pageSize);
}
