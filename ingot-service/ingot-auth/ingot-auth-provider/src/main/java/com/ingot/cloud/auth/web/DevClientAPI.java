package com.ingot.cloud.auth.web;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.auth.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.auth.model.dto.OAuth2RegisteredClientDTO;
import com.ingot.cloud.auth.service.domain.Oauth2RegisteredClientService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.commons.model.iam.DeveloperResources;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
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
@RequestMapping(value = "/client")
@RequiredArgsConstructor
public class DevClientAPI implements RShortcuts {
    private static final ScopeTarget GLOBAL_TARGET = new ScopeTarget(null, null, null, List.of());
    private final AuthorizationAccess access;
    private final Oauth2RegisteredClientService oauth2RegisteredClientService;

    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R<?> page(Page<Oauth2RegisteredClient> page, Oauth2RegisteredClient condition) {
        require(DeveloperResources.CLIENT_READ_LIST);
        return ok(oauth2RegisteredClientService.conditionPage(page, condition));
    }

    @Operation(summary = "详情", description = "详情")
    @GetMapping("/{id}")
    public R<?> getOne(@PathVariable String id) {
        require(DeveloperResources.CLIENT_READ_DETAIL);
        return ok(oauth2RegisteredClientService.getByClientId(id));
    }

    @Operation(summary = "创建", description = "创建")
    @PostMapping
    public R<?> create(@RequestBody @Validated(Group.Create.class) OAuth2RegisteredClientDTO params) {
        require(DeveloperResources.CLIENT_CREATE);
        return ok(oauth2RegisteredClientService.createClient(params));
    }

    @Operation(summary = "更新", description = "更新")
    @PutMapping
    public R<?> update(@RequestBody @Validated(Group.Update.class) OAuth2RegisteredClientDTO params) {
        require(DeveloperResources.CLIENT_UPDATE);
        oauth2RegisteredClientService.updateClientByClientId(params);
        return ok();
    }

    @Operation(summary = "删除", description = "删除")
    @DeleteMapping("/{clientId}")
    public R<?> removeById(@PathVariable String clientId) {
        require(DeveloperResources.CLIENT_DELETE);
        oauth2RegisteredClientService.removeClientByClientId(clientId);
        return ok();
    }

    @Operation(summary = "重置密钥", description = "重置密钥")
    @PutMapping("/secret/{clientId}")
    public R<?> resetSecret(@PathVariable String clientId) {
        require(DeveloperResources.CLIENT_RESET_SECRET);
        return ok(oauth2RegisteredClientService.resetSecret(clientId));
    }

    private void require(String action) {
        var decision = access.require(DeveloperResources.CLIENT, action);
        if (!DeveloperResources.CLIENT.equals(decision.resource())
                || decision.context().domain() != AuthorizationDomain.PLATFORM) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        access.requireTarget(decision, action, GLOBAL_TARGET);
    }
}
