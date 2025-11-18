package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.dto.common.BizBindDTO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface TenantRoleUserPrivateService extends BaseService<TenantRoleUserPrivate> {

    /**
     * 获取用户关联角色相关信息
     *
     * @param userId 用户ID
     * @return {@link TenantRoleUserPrivate}
     */
    List<TenantRoleUserPrivate> getUserRoles(long userId);

    /**
     * 角色绑定用户
     *
     * @param params {@link BizBindDTO}
     */
    void roleBindUsers(BizBindDTO params);

    /**
     * 根据角色ID清空角色用户关系
     *
     * @param id 角色ID
     */
    void clearByRoleId(long id);
}
