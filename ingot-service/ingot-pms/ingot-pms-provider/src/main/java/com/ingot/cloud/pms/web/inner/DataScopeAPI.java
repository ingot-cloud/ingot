package com.ingot.cloud.pms.web.inner;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : DataScopeAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/12.</p>
 * <p>Time         : 15:21.</p>
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/dataScope")
@RequiredArgsConstructor
public class DataScopeAPI implements RShortcuts {
    private final BizRoleService bizRoleService;
    private final BizDeptService bizDeptService;

    @PostMapping("/role/roleListByCodes")
    public R<List<RoleType>> getRoleListByCodes(@RequestBody List<String> roleCodeList) {
        return ok(bizRoleService.getRolesByCodes(roleCodeList));
    }

    @GetMapping("/dept/selfAndDescendantList/{deptId}")
    public R<List<TenantDept>> getSelfAndDescendantDeptList(@PathVariable("deptId") Long deptId) {
        return ok(bizDeptService.getDescendantList(deptId, true));
    }

    @GetMapping("/dept/userSelfAndDescendantDeptList/{userId}")
    public R<List<TenantDept>> getUserSelfAndDescendantDeptList(@PathVariable("userId") Long userId) {
        return ok(bizDeptService.getUserDescendant(userId, true));
    }

    @GetMapping("/dataScope/dept/userDeptIds/{userId}")
    R<List<Long>> getUserDeptIds(@PathVariable("userId") Long userId) {
        return ok(bizDeptService.getUserDeptIds(userId));
    }
}
