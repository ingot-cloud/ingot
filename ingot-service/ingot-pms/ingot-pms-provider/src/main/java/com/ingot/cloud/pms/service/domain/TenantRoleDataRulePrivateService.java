package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantRoleDataRulePrivate;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>租户追加数据规则的领域服务。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface TenantRoleDataRulePrivateService extends BaseService<TenantRoleDataRulePrivate> {

    /**
     * 读取当前租户下角色的追加数据规则。
     *
     * @param roleId       角色 ID
     * @param platformRole 是否平台预设角色
     * @return 规则列表，无规则时为空列表
     */
    List<TenantRoleDataRulePrivate> listByRole(long roleId, boolean platformRole);

    /**
     * 整体替换当前租户可管理的追加规则层，不触碰平台默认层。
     *
     * @param roleId       角色 ID
     * @param platformRole 是否平台预设角色
     * @param rules        完整追加规则集，可空表示清空该层
     */
    void replace(long roleId, boolean platformRole, List<TenantRoleDataRulePrivate> rules);
}
