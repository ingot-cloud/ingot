package com.ingot.cloud.pms.web.v1.platform.dev;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Tag(description = "DevSocial", name = "社交管理模块")
@RequestMapping(value = "/v1/platform/dev/social")
@RequiredArgsConstructor
public class DevSocialAPI implements RShortcuts {
    private final SysSocialDetailsService sysSocialDetailsService;
    private final WxMaService wxMaService;

    @Operation(summary = "查询列表", description = "查询列表")
    @AdminOrHasAnyAuthority({"platform:develop:social:query"})
    @GetMapping("/page")
    public R<?> page(Page<SysSocialDetails> page, SysSocialDetails condition) {
        return ok(sysSocialDetailsService.page(page, Wrappers.lambdaQuery(condition)));
    }

    @Operation(summary = "创建", description = "创建")
    @AdminOrHasAnyAuthority({"platform:develop:social:create"})
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public R<?> create(@RequestBody @Validated(Group.Create.class) SysSocialDetails params) {
        params.setTenantId(TenantContextHolder.get());
        params.setCreatedAt(DateUtil.now());
        params.setUpdatedAt(DateUtil.now());
        sysSocialDetailsService.save(params);

        // 更新 WxMaService 配置
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(params.getAppId());
        config.setSecret(params.getAppSecret());
        wxMaService.addConfig(params.getAppId(), config);
        return ok();
    }

    @Operation(summary = "更新", description = "更新")
    @AdminOrHasAnyAuthority({"platform:develop:social:update"})
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public R<?> update(@RequestBody @Validated(Group.Update.class) SysSocialDetails params) {
        params.setUpdatedAt(DateUtil.now());
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

    @Operation(summary = "删除", description = "删除")
    @AdminOrHasAnyAuthority({"platform:develop:social:delete"})
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
