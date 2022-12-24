package com.ingot.cloud.pms.mapper;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.framework.store.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleOauthClientMapper extends BaseMapper<SysRoleOauthClient> {

    /**
     * 创建用户客户端关系，如果已存在则忽略
     *
     * @param roleId   角色ID
     * @param clientId 客户端ID
     */
    void insertIgnore(@Param("roleId") long roleId, @Param("clientId") String clientId);

    /**
     * 获取指定角色绑定的所有客户端信息
     *
     * @param page      分页参数
     * @param roleId    角色ID
     * @param isBind    是否绑定
     * @param condition 条件参数
     * @return 客户端列表
     */
    List<Oauth2RegisteredClient> getRoleClients(@Param("roleId") long roleId,
                                                @Param("isBind") boolean isBind,
                                                @Param("condition") Oauth2RegisteredClient condition);

}
