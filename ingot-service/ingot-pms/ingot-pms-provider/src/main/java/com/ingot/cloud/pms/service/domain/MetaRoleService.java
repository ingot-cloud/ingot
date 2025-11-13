package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface MetaRoleService extends BaseService<MetaRole> {

    /**
     * 根据编码获取角色
     *
     * @param code 角色编码
     * @return {@link MetaRole}
     */
    MetaRole getByCode(String code);

    /**
     * 创建角色
     *
     * @param role {@link MetaRole}
     */
    void create(MetaRole role);

    /**
     * 创建角色并返回结果
     *
     * @param role {@link MetaRole}
     * @return {@link MetaRole}
     */
    MetaRole createAndReturnResult(MetaRole role);

    /**
     * 更新角色
     *
     * @param role {@link MetaRole}
     */
    void update(MetaRole role);

    /**
     * 删除角色, 只进行角色删除，不处理关联数据
     *
     * @param id 角色ID
     */
    void delete(long id);
}
