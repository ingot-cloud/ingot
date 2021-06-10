package com.ingot.cloud.pms.rest.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>Description  : TenantApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:13 下午.</p>
 */
@RestController(value = "/v1/tenant")
@AllArgsConstructor
public class TenantApi extends BaseController {
    private final SysTenantService sysTenantService;

    @GetMapping("/page")
    public IngotResponse<?> page(Page<SysTenant> page, SysTenant params) {
        return ok(sysTenantService.conditionPage(page, params));
    }

    @PostMapping
    public IngotResponse<?> create(@Valid @RequestBody SysTenant params) {
        sysTenantService.createTenant(params);
        return ok();
    }

    @PutMapping
    public IngotResponse<?> update(@Valid @RequestBody SysTenant params) {
        sysTenantService.updateTenantById(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public IngotResponse<?> removeById(@PathVariable Integer id) {
        sysTenantService.removeTenantById(id);
        return ok();
    }
}
