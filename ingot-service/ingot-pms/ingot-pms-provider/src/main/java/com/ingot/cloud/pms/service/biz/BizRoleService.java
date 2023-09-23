package com.ingot.cloud.pms.service.biz;

import com.ingot.framework.core.model.common.RelationDTO;

import java.util.List;

/**
 * <p>Description  : BizRoleService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 8:56 AM.</p>
 */
public interface BizRoleService {

    /**
     * 组织角色绑定用户
     * @param params {@link RelationDTO}
     */
    void orgRoleBindUsers(RelationDTO<Long, Long> params);

    /**
     * 设置用户角色
     * @param userId 用户ID
     * @param roles 角色
     */
    void setOrgUserRoles(long userId, List<Long> roles);
}
