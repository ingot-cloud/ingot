package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDTO;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.model.support.R;
import lombok.RequiredArgsConstructor;
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

    @PreAuthorize("@ingot.hasAnyAuthority('basic.client.write', 'basic.client.read')")
    @GetMapping("/page")
    public R<?> page(Page<Oauth2RegisteredClient> page, Oauth2RegisteredClient condition) {
        return ok(oauth2RegisteredClientService.conditionPage(page, condition));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.client.write')")
    @GetMapping("/{id}")
    public R<?> getOne(@PathVariable String id) {
        return ok(oauth2RegisteredClientService.getByClientId(id));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.client.write')")
    @PostMapping
    public R<?> create(@RequestBody @Validated(Group.Create.class) OAuth2RegisteredClientDTO params) {
        oauth2RegisteredClientService.createClient(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.client.write')")
    @PutMapping
    public R<?> update(@RequestBody @Validated(Group.Update.class) OAuth2RegisteredClientDTO params) {
        oauth2RegisteredClientService.updateClientByClientId(params);
        return ok();
    }

    @DeleteMapping("/{clientId}")
    public R<?> removeById(@PathVariable String clientId) {
        oauth2RegisteredClientService.removeClientByClientId(clientId);
        return ok();
    }
}
