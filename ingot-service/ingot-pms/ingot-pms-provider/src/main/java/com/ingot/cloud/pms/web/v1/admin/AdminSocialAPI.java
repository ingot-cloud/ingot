package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : SocialApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/1/19.</p>
 * <p>Time         : 10:19 AM.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/admin/social")
@RequiredArgsConstructor
public class AdminSocialAPI implements RShortcuts {
    private final SysSocialDetailsService sysSocialDetailsService;

    @PreAuthorize("@ingot.hasAnyAuthority('develop.social.w', 'develop.social.r')")
    @GetMapping("/page")
    public R<?> page(Page<SysSocialDetails> page, SysSocialDetails condition) {
        return ok(sysSocialDetailsService.page(page, Wrappers.lambdaQuery(condition)));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('develop.social.w')")
    @PostMapping
    public R<?> create(@RequestBody @Validated(Group.Create.class) SysSocialDetails params) {
        params.setTenantId(TenantContextHolder.get());
        params.setCreatedAt(DateUtils.now());
        params.setUpdatedAt(DateUtils.now());
        sysSocialDetailsService.save(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('develop.social.w')")
    @PutMapping
    public R<?> update(@RequestBody @Validated(Group.Update.class) SysSocialDetails params) {
        params.setUpdatedAt(DateUtils.now());
        sysSocialDetailsService.updateById(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('develop.social.w')")
    @DeleteMapping("/{id}")
    public R<?> remove(@PathVariable Long id) {
        sysSocialDetailsService.removeById(id);
        return ok();
    }
}
