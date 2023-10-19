package com.ingot.cloud.pms.web.v1.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.service.biz.BizOrgService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>Description  : TenantApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:13 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/admin/tenant")
@RequiredArgsConstructor
public class AdminTenantAPI implements RShortcuts {
    private final SysTenantService sysTenantService;
    private final BizOrgService bizOrgService;

    @GetMapping("/search")
    public R<?> search(SysTenant filter) {
        String name = filter.getName();
        if (StrUtil.isEmpty(name)) {
            return ok(ListUtil.empty());
        }

        List<SysTenant> list = CollUtil.emptyIfNull(
                sysTenantService.list(Wrappers.<SysTenant>lambdaQuery()
                        .like(SysTenant::getName, name)));

        return ok(list.stream()
                .filter(item -> item.getStatus() == CommonStatusEnum.ENABLE)
                .toList());
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.tenant.write', 'basic.tenant.read')")
    @GetMapping("/page")
    public R<?> page(Page<SysTenant> page, SysTenant params) {
        return ok(sysTenantService.conditionPage(page, params));
    }

    @PreAuthorize("@ingot.requiredAdmin")
    @PostMapping
    public R<?> create(@Valid @RequestBody CreateOrgDTO params) {
        bizOrgService.createOrg(params);
        return ok();
    }

    @PreAuthorize("@ingot.requiredAdmin")
    @PutMapping
    public R<?> update(@Valid @RequestBody SysTenant params) {
        bizOrgService.updateBase(params);
        return ok();
    }

    @PreAuthorize("@ingot.requiredAdmin")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizOrgService.removeOrg(id);
        return ok();
    }
}
