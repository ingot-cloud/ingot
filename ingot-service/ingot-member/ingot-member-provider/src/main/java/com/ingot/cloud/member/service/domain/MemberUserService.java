package com.ingot.cloud.member.service.domain;

import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
public interface MemberUserService extends BaseService<MemberUser> {

    /**
     * 创建用户（走账号域 {@code RegisterUserUseCase}，自动初始化密码历史/过期等附属记录）
     *
     * @param user {@link MemberUser}
     */
    void create(MemberUser user);

    /**
     * 更新用户基础信息
     * <p>密码字段会被忽略，密码修改请统一使用 {@code ChangePasswordUseCase}</p>
     *
     * @param user {@link MemberUser}
     */
    void update(MemberUser user);

    /**
     * 根据ID删除用户（软删除，走账号域 {@code DeleteAccountUseCase}）
     *
     * @param id 用户ID
     */
    void delete(long id);
}
