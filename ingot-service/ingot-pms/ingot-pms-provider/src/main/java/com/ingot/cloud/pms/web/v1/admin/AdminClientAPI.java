package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDTO;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.HasAnyAuthority;
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
@RequestMapping(value = "/v1/admin/client")
@RequiredArgsConstructor
public class AdminClientAPI implements RShortcuts {
    private final Oauth2RegisteredClientService oauth2RegisteredClientService;

    @HasAnyAuthority({"develop:client:w", "develop:client:r"})
    @GetMapping("/page")
    public R<?> page(Page<Oauth2RegisteredClient> page, Oauth2RegisteredClient condition) {
        return ok(oauth2RegisteredClientService.conditionPage(page, condition));
    }

    @HasAnyAuthority({"develop:client:w"})
    @GetMapping("/{id}")
    public R<?> getOne(@PathVariable String id) {
        return ok(oauth2RegisteredClientService.getByClientId(id));
    }

    @HasAnyAuthority({"develop:client:w"})
    @PostMapping
    public R<?> create(@RequestBody @Validated(Group.Create.class) OAuth2RegisteredClientDTO params) {
        return ok(oauth2RegisteredClientService.createClient(params));
    }

    @HasAnyAuthority({"develop:client:w"})
    @PutMapping
    public R<?> update(@RequestBody @Validated(Group.Update.class) OAuth2RegisteredClientDTO params) {
        oauth2RegisteredClientService.updateClientByClientId(params);
        return ok();
    }

    @HasAnyAuthority({"develop:client:w"})
    @DeleteMapping("/{clientId}")
    public R<?> removeById(@PathVariable String clientId) {
        oauth2RegisteredClientService.removeClientByClientId(clientId);
        return ok();
    }

    @HasAnyAuthority({"develop:client:w"})
    @PutMapping("/resetSecret/{clientId}")
    public R<?> resetSecret(@PathVariable String clientId) {
        return ok(oauth2RegisteredClientService.resetSecret(clientId));
    }
}
