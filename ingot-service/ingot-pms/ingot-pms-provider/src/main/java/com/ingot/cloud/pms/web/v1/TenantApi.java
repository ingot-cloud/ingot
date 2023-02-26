package com.ingot.cloud.pms.web.v1;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.model.support.Option;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : TenantApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:13 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/tenant")
@RequiredArgsConstructor
public class TenantApi implements RShortcuts {
    private final SysTenantService sysTenantService;

    @Permit
    @GetMapping("/options")
    public R<?> options() {
        List<SysTenant> list = CollUtil.emptyIfNull(sysTenantService.list());
        return ok(list.stream()
                .filter(item -> item.getStatus() == CommonStatusEnum.ENABLE)
                .map(item -> Option.of(item.getId(), item.getName()))
                .collect(Collectors.toList()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.tenant.write', 'basic.tenant.read')")
    @GetMapping("/page")
    public R<?> page(Page<SysTenant> page, SysTenant params) {
        return ok(sysTenantService.conditionPage(page, params));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.tenant.write')")
    @PostMapping
    public R<?> create(@Valid @RequestBody SysTenant params) {
        sysTenantService.createTenant(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.tenant.write')")
    @PutMapping
    public R<?> update(@Valid @RequestBody SysTenant params) {
        sysTenantService.updateTenantById(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.tenant.write')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        sysTenantService.removeTenantById(id);
        return ok();
    }
}
