package com.ingot.cloud.pms.rest.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysOauthClientDetails;
import com.ingot.cloud.pms.service.domain.SysOauthClientDetailsService;
import com.ingot.framework.core.validation.Group;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : ClientApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/30.</p>
 * <p>Time         : 10:12 下午.</p>
 */
@RestController
@RequestMapping(value = "/v1/client")
@AllArgsConstructor
public class ClientApi extends BaseController {
    private final SysOauthClientDetailsService sysOauthClientDetailsService;

    @GetMapping("/page")
    public IngotResponse<?> page(Page<SysOauthClientDetails> page, SysOauthClientDetails condition) {
        return ok(sysOauthClientDetailsService.conditionPage(page, condition));
    }

    @PostMapping
    public IngotResponse<?> create(@RequestBody @Validated(Group.Create.class) SysOauthClientDetails params) {
        sysOauthClientDetailsService.createClient(params);
        return ok();
    }

    @PutMapping
    public IngotResponse<?> update(@RequestBody @Validated(Group.Update.class) SysOauthClientDetails params) {
        sysOauthClientDetailsService.updateClient(params);
        return ok();
    }

    @DeleteMapping("/{id}")
    public IngotResponse<?> removeById(@PathVariable Long id) {
        sysOauthClientDetailsService.removeClientById(id);
        return ok();
    }
}
