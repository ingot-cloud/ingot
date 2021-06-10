package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.framework.store.mybatis.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysAuthorityService extends BaseService<SysAuthority> {

    /**
     * 创建权限
     *
     * @param params 参数
     */
    void createAuthority(SysAuthority params);

    /**
     * 更新权限
     *
     * @param params 更新参数
     */
    void updateAuthority(SysAuthority params);

    /**
     * 删除权限
     *
     * @param id 权限ID
     */
    void removeAuthorityById(long id);
}
