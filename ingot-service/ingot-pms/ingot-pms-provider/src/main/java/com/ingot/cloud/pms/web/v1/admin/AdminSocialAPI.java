package com.ingot.cloud.pms.web.v1.admin;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    private final WxMaService wxMaService;

    @PreAuthorize("@ingot.hasAnyAuthority('develop.social.w', 'develop.social.r')")
    @GetMapping("/page")
    public R<?> page(Page<SysSocialDetails> page, SysSocialDetails condition) {
        return ok(sysSocialDetailsService.page(page, Wrappers.lambdaQuery(condition)));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('develop.social.w')")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public R<?> create(@RequestBody @Validated(Group.Create.class) SysSocialDetails params) {
        params.setTenantId(TenantContextHolder.get());
        params.setCreatedAt(DateUtils.now());
        params.setUpdatedAt(DateUtils.now());
        sysSocialDetailsService.save(params);

        // 更新 WxMaService 配置
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(params.getAppId());
        config.setSecret(params.getAppSecret());
        wxMaService.addConfig(params.getAppId(), config);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('develop.social.w')")
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public R<?> update(@RequestBody @Validated(Group.Update.class) SysSocialDetails params) {
        params.setUpdatedAt(DateUtils.now());
        sysSocialDetailsService.updateById(params);

        // 如果更新了secret，那么需要重新add
        if (StringUtils.isNotEmpty(params.getAppSecret())) {
            SysSocialDetails current = sysSocialDetailsService.getById(params.getId());
            // 更新 WxMaService 配置
            WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
            config.setAppid(current.getAppId());
            config.setSecret(current.getAppSecret());
            wxMaService.addConfig(current.getAppId(), config);
        }
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('develop.social.w')")
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public R<?> remove(@PathVariable Long id) {

        SysSocialDetails current = sysSocialDetailsService.getById(id);
        if (current != null) {
            wxMaService.removeConfig(current.getAppId());
        }
        sysSocialDetailsService.removeById(id);
        return ok();
    }
}
