package com.ingot.cloud.pms.rest.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.service.SysRoleService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.base.exception.IllegalOperationException;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : RoleApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/22.</p>
 * <p>Time         : 4:41 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/role")
@AllArgsConstructor
public class RoleApi extends BaseController {
    private final SysRoleService sysRoleService;
    private final IdGenerator idGenerator;

    @GetMapping("page")
    public IngotResponse<?> page(Page<SysRole> page, SysRole condition) {
        return ok(sysRoleService.conditionPage(page, condition));
    }

    @PostMapping
    public IngotResponse<?> create(@Validated SysRole params) {
        params.setId(idGenerator.nextId());
        boolean result = sysRoleService.save(params);
        if (!result) {
            throw new IllegalOperationException("角色创建失败");
        }
        return ok();
    }
}
