package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface TenantRolePrivateService extends BaseService<TenantRolePrivate> {
    /**
     * 根据编码获取角色
     *
     * @param code 角色编码
     * @return {@link TenantRolePrivate}
     */
    TenantRolePrivate getByCode(String code);

    /**
     * 创建角色
     *
     * @param role {@link TenantRolePrivate}
     */
    void create(TenantRolePrivate role);

    /**
     * 创建角色并返回结果
     *
     * @param role {@link TenantRolePrivate}
     * @return {@link TenantRolePrivate}
     */
    TenantRolePrivate createAndReturnResult(TenantRolePrivate role);

    /**
     * 更新角色
     *
     * @param role {@link TenantRolePrivate}
     */
    void update(TenantRolePrivate role);

    /**
     * 删除角色, 只进行角色删除，不处理关联数据
     *
     * @param id 角色ID
     */
    void delete(long id);
}
