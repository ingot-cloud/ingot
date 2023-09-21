package com.ingot.cloud.pms.service.biz;

import com.ingot.framework.core.model.common.RelationDTO;

/**
 * <p>Description  : BizRoleService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 8:56 AM.</p>
 */
public interface BizRoleService {

    /**
     * 角色绑定用户
     * @param params {@link RelationDTO}
     */
    void roleBindUsers(RelationDTO<Long, Long> params);
}
