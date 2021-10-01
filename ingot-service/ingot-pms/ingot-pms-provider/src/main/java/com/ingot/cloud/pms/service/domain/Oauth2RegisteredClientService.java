package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.framework.store.mybatis.service.BaseService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2021-09-29
 */
public interface Oauth2RegisteredClientService extends BaseService<Oauth2RegisteredClient> {

    /**
     * 根据角色id获取对应可用的client
     *
     * @param roleIds 角色ID
     * @return Oauth2RegisteredClient 列表
     */
    List<Oauth2RegisteredClient> getClientsByRoles(@Param("list") List<Long> roleIds);
}
