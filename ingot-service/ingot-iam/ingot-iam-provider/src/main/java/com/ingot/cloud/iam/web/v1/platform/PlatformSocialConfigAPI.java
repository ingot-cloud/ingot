package com.ingot.cloud.iam.web.v1.platform;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.api.model.domain.SysSocialDetails;
import com.ingot.cloud.iam.service.domain.SysSocialDetailsService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
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
 * <p>以平台精确 ACTION 接入既有社会化登录配置，不改写密钥保护与绑定逻辑。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 社会化登录配置")
@RequestMapping("/v1/platform/social-configs")
@RequiredArgsConstructor
public class PlatformSocialConfigAPI implements RShortcuts {
    private final IamAccess access;
    private final SysSocialDetailsService socials;
    private final WxMaService wxMaService;

    /**
     * 分页查询社会化登录配置。
     *
     * @param page 分页
     * @param condition 查询条件
     * @return 配置页
     */
    @Operation(summary = "社会化登录配置列表")
    @GetMapping
    public R<?> list(Page<SysSocialDetails> page, SysSocialDetails condition) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_SOCIAL_CONFIG_READ);
        return ok(socials.page(page, Wrappers.lambdaQuery(condition)));
    }

    /**
     * 创建社会化登录配置。
     *
     * @param params 配置
     * @return 空成功
     */
    @Operation(summary = "创建社会化登录配置")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public R<Void> create(@RequestBody @Validated(Group.Create.class) SysSocialDetails params) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_SOCIAL_CONFIG_CREATE);
        params.setTenantId(TenantContextHolder.get());
        params.setCreatedAt(DateUtil.now());
        params.setUpdatedAt(DateUtil.now());
        socials.save(params);
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(params.getAppId());
        config.setSecret(params.getAppSecret());
        wxMaService.addConfig(params.getAppId(), config);
        return ok();
    }

    /**
     * 更新社会化登录配置。
     *
     * @param id 配置 ID
     * @param params 配置
     * @return 空成功
     */
    @Operation(summary = "更新社会化登录配置")
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> update(@PathVariable Long id, @RequestBody @Validated(Group.Update.class) SysSocialDetails params) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_SOCIAL_CONFIG_UPDATE);
        params.setId(id);
        params.setUpdatedAt(DateUtil.now());
        socials.updateById(params);
        if (StringUtils.isNotEmpty(params.getAppSecret())) {
            SysSocialDetails current = socials.getById(id);
            WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
            config.setAppid(current.getAppId());
            config.setSecret(current.getAppSecret());
            wxMaService.addConfig(current.getAppId(), config);
        }
        return ok();
    }

    /**
     * 删除社会化登录配置。
     *
     * @param id 配置 ID
     * @return 空成功
     */
    @Operation(summary = "删除社会化登录配置")
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> remove(@PathVariable Long id) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_SOCIAL_CONFIG_DELETE);
        SysSocialDetails current = socials.getById(id);
        if (current != null) {
            wxMaService.removeConfig(current.getAppId());
        }
        socials.removeById(id);
        return ok();
    }
}
