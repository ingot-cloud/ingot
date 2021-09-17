package com.ingot.cloud.pms.rest.v1;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.vo.tenant.SimpleTenantVo;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Description  : TenantApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:13 下午.</p>
 */
@RestController
@RequestMapping(value = "/v1/tenant")
@AllArgsConstructor
public class TenantApi extends BaseController {
    private final SysTenantService sysTenantService;

    @Permit
    @GetMapping("/list")
    public R<?> list(){
        List<SysTenant> list = sysTenantService.list();
        if (CollUtil.isEmpty(list)) {
            list = CollUtil.newArrayList();
        }

        return ok(list.stream().map(SimpleTenantVo::new).collect(Collectors.toList()));
    }

    @GetMapping("/page")
    public R<?> page(Page<SysTenant> page, SysTenant params) {
        return ok(sysTenantService.conditionPage(page, params));
    }

    @PostMapping
    public R<?> create(@Valid @RequestBody SysTenant params) {
        sysTenantService.createTenant(params);
        return ok();
    }

    @PutMapping
    public R<?> update(@Valid @RequestBody SysTenant params) {
        sysTenantService.updateTenantById(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Integer id) {
        sysTenantService.removeTenantById(id);
        return ok();
    }
}
