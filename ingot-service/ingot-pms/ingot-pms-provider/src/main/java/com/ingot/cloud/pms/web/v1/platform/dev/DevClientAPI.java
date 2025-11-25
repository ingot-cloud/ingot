package com.ingot.cloud.pms.web.v1.platform.dev;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDTO;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : ClientApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:12 下午.</p>
 */
@RestController
@Tag(description = "client", name = "OAuth2客户端管理模块")
@RequestMapping(value = "/v1/platform/dev/client")
@RequiredArgsConstructor
public class DevClientAPI implements RShortcuts {
    private final Oauth2RegisteredClientService oauth2RegisteredClientService;

    @Operation(summary = "分页查询", description = "分页查询")
    @AdminOrHasAnyAuthority({"platform:develop:client:query"})
    @GetMapping("/page")
    public R<?> page(Page<Oauth2RegisteredClient> page, Oauth2RegisteredClient condition) {
        return ok(oauth2RegisteredClientService.conditionPage(page, condition));
    }

    @Operation(summary = "详情", description = "详情")
    @AdminOrHasAnyAuthority({"platform:develop:client:detail"})
    @GetMapping("/{id}")
    public R<?> getOne(@PathVariable String id) {
        return ok(oauth2RegisteredClientService.getByClientId(id));
    }

    @Operation(summary = "创建", description = "创建")
    @AdminOrHasAnyAuthority({"platform:develop:client:create"})
    @PostMapping
    public R<?> create(@RequestBody @Validated(Group.Create.class) OAuth2RegisteredClientDTO params) {
        return ok(oauth2RegisteredClientService.createClient(params));
    }

    @Operation(summary = "更新", description = "更新")
    @AdminOrHasAnyAuthority({"platform:develop:client:update"})
    @PutMapping
    public R<?> update(@RequestBody @Validated(Group.Update.class) OAuth2RegisteredClientDTO params) {
        oauth2RegisteredClientService.updateClientByClientId(params);
        return ok();
    }

    @Operation(summary = "删除", description = "删除")
    @AdminOrHasAnyAuthority({"platform:develop:client:delete"})
    @DeleteMapping("/{clientId}")
    public R<?> removeById(@PathVariable String clientId) {
        oauth2RegisteredClientService.removeClientByClientId(clientId);
        return ok();
    }

    @Operation(summary = "重置密钥", description = "重置密钥")
    @AdminOrHasAnyAuthority({"platform:develop:client:reset"})
    @PutMapping("/secret/{clientId}")
    public R<?> resetSecret(@PathVariable String clientId) {
        return ok(oauth2RegisteredClientService.resetSecret(clientId));
    }
}
