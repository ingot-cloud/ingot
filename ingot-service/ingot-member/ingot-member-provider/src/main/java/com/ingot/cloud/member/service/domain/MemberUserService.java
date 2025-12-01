package com.ingot.cloud.member.service.domain;

import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.dto.user.MemberUserPasswordDTO;
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
     * 创建用户
     *
     * @param user {@link MemberUser}
     */
    void create(MemberUser user);

    /**
     * 更新用户
     *
     * @param user {@link MemberUser}
     */
    void update(MemberUser user);

    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     */
    void delete(long id);

    /**
     * 用户修改密码
     *
     * @param id     用户ID
     * @param params 参数
     */
    void fixPassword(long id, MemberUserPasswordDTO params);
}
