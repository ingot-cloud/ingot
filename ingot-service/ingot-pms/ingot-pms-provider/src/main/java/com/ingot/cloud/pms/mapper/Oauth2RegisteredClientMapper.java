package com.ingot.cloud.pms.mapper;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.framework.store.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2021-09-29
 */
public interface Oauth2RegisteredClientMapper extends BaseMapper<Oauth2RegisteredClient> {
    /**
     * 根据角色id获取对应可用的client
     *
     * @param roleIds 角色ID
     * @return Oauth2RegisteredClient 列表
     */
    List<Oauth2RegisteredClient> getClientsByRoles(@Param("list") List<Long> roleIds,
                                                   @Param("status") String status);
}
