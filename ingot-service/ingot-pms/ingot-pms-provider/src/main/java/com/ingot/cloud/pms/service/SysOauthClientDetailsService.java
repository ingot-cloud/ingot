package com.ingot.cloud.pms.service;

import com.ingot.cloud.pms.model.domain.SysOauthClientDetails;
import com.ingot.framework.store.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysOauthClientDetailsService extends BaseService<SysOauthClientDetails> {

    /**
     * 获取指定角色绑定的所有client
     *
     * @param roleIds 角色ID列表
     * @return client 列表
     */
    List<SysOauthClientDetails> getClientsByRoles(List<Long> roleIds);
}
