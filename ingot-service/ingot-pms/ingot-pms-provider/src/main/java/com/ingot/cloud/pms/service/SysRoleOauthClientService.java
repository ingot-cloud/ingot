package com.ingot.cloud.pms.service;

import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.framework.core.model.dto.common.RelationDto;
import com.ingot.framework.store.mybatis.service.BaseService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleOauthClientService extends BaseService<SysRoleOauthClient> {

    /**
     * 客户端绑定角色
     *
     * @param params 关联参数
     */
    void clientBindRoles(RelationDto<Long, Long> params);

    /**
     * 角色绑定客户端
     *
     * @param params 关联参数
     */
    void roleBindClients(RelationDto<Long, Long> params);
}
