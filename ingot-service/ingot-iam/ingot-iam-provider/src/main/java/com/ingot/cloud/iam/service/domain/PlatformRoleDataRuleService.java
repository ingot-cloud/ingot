package com.ingot.cloud.iam.service.domain;

import java.util.List;

import com.ingot.cloud.iam.api.model.domain.PlatformRoleDataRule;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>平台角色默认数据规则的领域服务。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface PlatformRoleDataRuleService extends BaseService<PlatformRoleDataRule> {

    /**
     * 读取角色的平台默认数据规则。
     *
     * @param roleId 平台角色 ID
     * @return 规则列表，无规则时为空列表
     */
    List<PlatformRoleDataRule> listByRoleId(long roleId);

    /**
     * 整体替换角色的平台默认数据规则层。
     *
     * @param roleId 平台角色 ID
     * @param rules  完整规则集，可空表示清空该层
     */
    void replace(long roleId, List<PlatformRoleDataRule> rules);
}
