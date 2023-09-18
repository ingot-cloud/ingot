package com.ingot.cloud.pms.service.biz;

import java.util.List;

/**
 * <p>Description  : BizDeptService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/18.</p>
 * <p>Time         : 4:26 PM.</p>
 */
public interface BizDeptService {

    /**
     * 设置用户部门, 确保包含主要部门
     * @param userId 用户ID
     * @param deptIds 待设置的部门，非主要部门
     */
    void setUserDeptsEnsureMainDept(long userId, List<Long> deptIds);
}
