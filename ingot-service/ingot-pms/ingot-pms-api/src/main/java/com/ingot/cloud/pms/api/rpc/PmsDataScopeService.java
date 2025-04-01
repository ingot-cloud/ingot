package com.ingot.cloud.pms.api.rpc;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.framework.core.constants.ServiceNameConstants;
import com.ingot.framework.core.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <p>Description  : PmsDataScopeService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/12.</p>
 * <p>Time         : 15:17.</p>
 */
@FeignClient(contextId = "pmsDataScopeService", value = ServiceNameConstants.PMS_SERVICE)
public interface PmsDataScopeService {

    /**
     * 通过角色编码查询角色列表
     *
     * @param roleCodeList 角色编码
     */
    @PostMapping("/dataScope/role/getRoleListByCodes")
    R<List<SysRole>> getRoleListByCodes(@RequestBody List<String> roleCodeList);

    /**
     * 获取子级部门以及当前部门信息
     *
     * @param deptId 部门ID
     */
    @GetMapping("/dataScope/dept/getSelfAndDescendantList/{deptId}")
    R<List<SysDept>> getSelfAndDescendantList(@PathVariable("deptId") Long deptId);

    /**
     * 获取用户当前部门以及所有子部门信息
     *
     * @param userId 用户ID
     */
    @GetMapping("/dept/getUserSelfAndDescendantDeptList/{userId}")
    R<List<SysDept>> getUserSelfAndDescendantDeptList(@PathVariable("userId") Long userId);

    @GetMapping("/dataScope/dept/getUserDeptIds/{userId}")
    R<List<Long>> getUserDeptIds(@PathVariable("userId") Long userId);
}
