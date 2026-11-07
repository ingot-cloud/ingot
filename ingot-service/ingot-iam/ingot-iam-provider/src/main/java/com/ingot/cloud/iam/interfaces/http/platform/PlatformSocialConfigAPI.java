package com.ingot.cloud.iam.interfaces.http.platform;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.application.social.model.SocialConfigInput;
import com.ingot.cloud.iam.application.social.model.SocialConfigRecord;
import com.ingot.cloud.iam.application.social.SocialConfigService;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    private final SocialConfigService socials;

    /**
     * 分页查询社会化登录配置。
     *
     * @param page 分页
     * @param condition 查询条件
     * @return 配置页
     */
    @Operation(summary = "社会化登录配置列表")
    @GetMapping
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_SOCIAL_CONFIG_READ)
    public R<?> list(Page<SocialConfigRecord> page, SocialConfigInput condition) {
        return ok(socials.list(page, condition));
    }

    /**
     * 创建社会化登录配置。
     *
     * @param params 配置
     * @return 空成功
     */
    @Operation(summary = "创建社会化登录配置")
    @PostMapping
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_SOCIAL_CONFIG_CREATE)
    public R<Void> create(@RequestBody @Validated(Group.Create.class) SocialConfigInput params) {
        socials.create(params);
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
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_SOCIAL_CONFIG_UPDATE)
    public R<Void> update(@PathVariable Long id, @RequestBody @Validated(Group.Update.class) SocialConfigInput params) {
        socials.update(id, params);
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
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_SOCIAL_CONFIG_DELETE)
    public R<Void> remove(@PathVariable Long id) {
        socials.remove(id);
        return ok();
    }
}
