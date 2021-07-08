package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysOauthClientDetails;
import com.ingot.framework.store.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysOauthClientDetailsService extends BaseService<SysOauthClientDetails> {

    /**
     * 获取指定角色绑定的所有client
     *
     * @param roleIds 角色ID列表
     * @return client 列表
     */
    List<SysOauthClientDetails> getClientsByRoles(List<Long> roleIds);

    /**
     * 条件查询
     *
     * @param page      分页参数
     * @param condition 条件参数
     * @return {@link IPage}，数据项结构 {@link SysOauthClientDetails}
     */
    IPage<SysOauthClientDetails> conditionPage(Page<SysOauthClientDetails> page,
                                               SysOauthClientDetails condition);

    /**
     * 创建客户端
     * @param params 参数
     */
    void createClient(SysOauthClientDetails params);

    /**
     * 更新客户端
     * @param params 更新参数
     */
    void updateClientByClientId(SysOauthClientDetails params);

    /**
     * 根据ID删除客户端
     * @param clientId clientId
     */
    void removeClientByClientId(String clientId);
}
