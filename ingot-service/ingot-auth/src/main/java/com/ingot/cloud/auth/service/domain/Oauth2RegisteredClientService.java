package com.ingot.cloud.auth.service.domain;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.auth.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.auth.model.dto.OAuth2RegisteredClientDTO;
import com.ingot.cloud.auth.model.vo.AppSecretVO;
import com.ingot.cloud.auth.model.vo.OAuth2RegisteredClientVO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

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
     * 条件list
     *
     * @param condition 条件
     * @return {@link Oauth2RegisteredClient} List
     */
    List<Oauth2RegisteredClient> list(Oauth2RegisteredClient condition);

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
     * @param id 客户端表ID，并非clientId
     * @return {@link OAuth2RegisteredClientVO}
     */
    OAuth2RegisteredClientVO getByClientId(String id);

    /**
     * 创建客户端
     *
     * @param params 参数
     * @return {@link AppSecretVO}
     */
    AppSecretVO createClient(OAuth2RegisteredClientDTO params);

    /**
     * 更新客户端
     *
     * @param params 更新参数
     */
    void updateClientByClientId(OAuth2RegisteredClientDTO params);

    /**
     * 根据ID删除客户端
     *
     * @param id 客户端表ID，并非clientId
     */
    void removeClientByClientId(String id);

    /**
     * 根据客户端ID充值secret
     *
     * @param id app id
     * @return {@link AppSecretVO}
     */
    AppSecretVO resetSecret(String id);
}
