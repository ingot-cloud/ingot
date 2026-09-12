package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformRoleDataRule;
import com.ingot.cloud.pms.api.model.domain.TenantRoleDataRulePrivate;
import com.ingot.cloud.pms.api.model.dto.role.RoleDataRuleItemDTO;
import com.ingot.cloud.pms.api.model.dto.role.RoleDataRuleSetDTO;

/**
 * <p>角色数据规则读写，平台入口只写默认层，租户入口只写追加层。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface BizRoleDataRuleService {

    /**
     * 读取平台角色默认数据规则。
     *
     * @param roleId 平台角色 ID
     * @return 规则列表
     */
    List<PlatformRoleDataRule> listPlatform(long roleId);

    /**
     * 整体替换平台角色默认数据规则。CUSTOM 不得含租户部门。
     *
     * @param roleId 平台角色 ID
     * @param dto    完整规则集
     */
    void replacePlatform(long roleId, RoleDataRuleSetDTO dto);

    /**
     * 读取当前租户可管理的追加数据规则，自动识别角色来源。
     *
     * @param roleId 角色 ID
     * @return 追加规则列表
     */
    List<TenantRoleDataRulePrivate> listTenant(long roleId);

    /**
     * 整体替换当前租户追加规则层。
     *
     * @param roleId 角色 ID
     * @param dto    完整追加规则集
     */
    void replaceTenant(long roleId, RoleDataRuleSetDTO dto);
}
