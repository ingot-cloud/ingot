package com.ingot.cloud.pms.api.model.vo.biz;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : UserOrgInfoVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/21.</p>
 * <p>Time         : 11:21 AM.</p>
 */
@Data
public class UserOrgInfoVO implements Serializable {
    /**
     * 组织ID
     */
    private Long orgId;
    /**
     * 部门ID
     */
    private List<Long> deptIds;
    /**
     * 角色ID
     */
    private List<Long> roleIds;
}
