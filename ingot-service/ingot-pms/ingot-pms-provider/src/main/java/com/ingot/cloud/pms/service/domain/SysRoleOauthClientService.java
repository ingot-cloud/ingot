package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysOauthClientDetails;
import com.ingot.cloud.pms.api.model.domain.SysRoleOauthClient;
import com.ingot.framework.core.model.dto.common.RelationDto;
import com.ingot.framework.store.mybatis.service.BaseService;

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
    void clientBindRoles(RelationDto<Long, Long> params);

    /**
     * 角色绑定客户端
     *
     * @param params 关联参数
     */
    void roleBindClients(RelationDto<Long, Long> params);

    /**
     * 获取角色客户端
     *
     * @param roleId    角色id
     * @param page      分页信息
     * @param isBind    是否绑定
     * @param condition 条件参数
     * @return 分页用户
     */
    IPage<SysOauthClientDetails> getRoleClients(long roleId,
                                                Page<?> page,
                                                boolean isBind,
                                                SysOauthClientDetails condition);
}
