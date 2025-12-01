package com.ingot.cloud.member.service.domain;

import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
public interface MemberRoleService extends BaseService<MemberRole> {

    /**
     * 根据编码获取角色
     *
     * @param code 角色编码
     * @return {@link MemberRole}
     */
    MemberRole getByCode(String code);

    /**
     * 创建角色
     *
     * @param role {@link MemberRole}
     */
    void create(MemberRole role);

    /**
     * 创建角色并返回结果
     *
     * @param role {@link MemberRole}
     * @return {@link MemberRole}
     */
    MemberRole createAndReturnResult(MemberRole role);

    /**
     * 更新角色
     *
     * @param role {@link MemberRole}
     */
    void update(MemberRole role);

    /**
     * 删除角色, 只进行角色删除，不处理关联数据
     *
     * @param id 角色ID
     */
    void delete(long id);
}
