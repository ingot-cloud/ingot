package com.ingot.cloud.pms.mapper;

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
    void insertIgnore(@Param("roleId") long roleId, @Param("clientId") long clientId);
}
