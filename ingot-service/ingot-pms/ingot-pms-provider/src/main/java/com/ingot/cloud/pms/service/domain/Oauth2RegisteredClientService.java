package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDTO;
import com.ingot.cloud.pms.api.model.vo.client.OAuth2RegisteredClientVO;
import com.ingot.framework.store.mybatis.service.BaseService;
import org.apache.ibatis.annotations.Param;

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
    List<Oauth2RegisteredClient> getClientsByRoles(@Param("list") List<Integer> roleIds);

    /**
     * 条件查询
     *
     * @param page      分页参数
     * @param condition 条件参数
     * @return {@link IPage}，数据项结构 {@link Oauth2RegisteredClient}
     */
    IPage<OAuth2RegisteredClientVO> conditionPage(Page<Oauth2RegisteredClient> page,
                                                  Oauth2RegisteredClient condition);

    /**
     * 根据ID获取相关信息
     *
     * @param clientId ClientId
     * @return {@link OAuth2RegisteredClientVO}
     */
    OAuth2RegisteredClientVO getByClientId(String clientId);

    /**
     * 创建客户端
     *
     * @param params 参数
     */
    void createClient(OAuth2RegisteredClientDTO params);

    /**
     * 更新客户端
     *
     * @param params 更新参数
     */
    void updateClientByClientId(OAuth2RegisteredClientDTO params);

    /**
     * 根据ID删除客户端
     *
     * @param clientId clientId
     */
    void removeClientByClientId(String clientId);
}
