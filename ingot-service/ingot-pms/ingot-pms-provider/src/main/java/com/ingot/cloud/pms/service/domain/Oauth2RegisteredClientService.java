package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDto;
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

    /**
     * 条件查询
     *
     * @param page      分页参数
     * @param condition 条件参数
     * @return {@link IPage}，数据项结构 {@link Oauth2RegisteredClient}
     */
    IPage<Oauth2RegisteredClient> conditionPage(Page<Oauth2RegisteredClient> page,
                                                Oauth2RegisteredClient condition);

    /**
     * 创建客户端
     *
     * @param params 参数
     */
    void createClient(OAuth2RegisteredClientDto params);

    /**
     * 更新客户端
     *
     * @param params 更新参数
     */
    void updateClientByClientId(OAuth2RegisteredClientDto params);

    /**
     * 根据ID删除客户端
     *
     * @param clientId clientId
     */
    void removeClientByClientId(String clientId);
}
