package com.ingot.cloud.pms.web.inner;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final SysRoleService sysRoleService;
    private final SysDeptService sysDeptService;

    @PostMapping("/role/getRoleListByCodes")
    public R<List<SysRole>> getRoleListByCodes(@RequestBody List<String> roleCodeList) {
        return ok(sysRoleService.getRoleListByCodes(roleCodeList));
    }

    @GetMapping("/dept/getSelfAndDescendantList/{deptId}")
    public R<List<SysDept>> getSelfAndDescendantDeptList(@PathVariable("deptId") Long deptId) {
        return ok(sysDeptService.getDescendantList(deptId, true));
    }

    @GetMapping("/dept/getUserSelfAndDescendantDeptList/{userId}")
    public R<List<SysDept>> getUserSelfAndDescendantDeptList(@PathVariable("userId") Long userId) {
        return ok(sysDeptService.getUserDescendant(userId, true));
    }

    @GetMapping("/dataScope/dept/getUserDeptIds/{userId}")
    R<List<Long>> getUserDeptIds(@PathVariable("userId") Long userId) {
        return ok(sysDeptService.getUserDeptIds(userId));
    }
}
