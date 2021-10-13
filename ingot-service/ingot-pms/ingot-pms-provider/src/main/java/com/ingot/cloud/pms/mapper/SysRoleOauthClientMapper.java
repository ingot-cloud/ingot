package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    void insertIgnore(@Param("roleId") long roleId, @Param("clientId") long clientId);

    /**
     * 获取角色客户端信息
     *
     * @param page      分页参数
     * @param roleId    角色ID
     * @param isBind    是否绑定
     * @param condition 条件参数
     * @return 分页用信息
     */
    IPage<Oauth2RegisteredClient> getRoleClients(Page<?> page,
                                                 @Param("roleId") long roleId,
                                                 @Param("isBind") boolean isBind,
                                                 @Param("condition") Oauth2RegisteredClient condition);

}
