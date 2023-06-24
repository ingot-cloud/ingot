package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import com.ingot.framework.data.mybatis.service.BaseService;

/**
 * <p>
 * 服务类
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
    void clientBindRoles(RelationDTO<String, Long> params);

    /**
     * 角色绑定客户端
     *
     * @param params 关联参数
     */
    void roleBindClients(RelationDTO<Long, String> params);

    /**
     * 获取角色客户端
     *
     * @param roleId 角色ID
     * @return {@link Oauth2RegisteredClient} List
     */
    List<Oauth2RegisteredClient> getRoleClients(long roleId);

    /**
     * 获取角色客户端
     *
     * @param roleId    角色id
     * @param condition 条件参数
     * @return {@link Oauth2RegisteredClient} List
     */
    List<Oauth2RegisteredClient> getRoleClients(long roleId,
                                                Oauth2RegisteredClient condition);
}
