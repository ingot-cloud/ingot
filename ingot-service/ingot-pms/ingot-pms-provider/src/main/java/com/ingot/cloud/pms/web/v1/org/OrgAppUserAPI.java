package com.ingot.cloud.pms.web.v1.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.service.biz.BizAppUserService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : OrgAppUserAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/26.</p>
 * <p>Time         : 11:02 AM.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/org/appUser")
@RequiredArgsConstructor
public class OrgAppUserAPI implements RShortcuts {
    private final BizAppUserService bizAppUserService;

    @GetMapping("/page")
    public R<?> page(Page<AppUser> page, AppUser condition) {
        return ok(bizAppUserService.page(page, condition));
    }

    @PutMapping
    public R<?> update(@RequestBody AppUser params) {
        bizAppUserService.updateUser(params);
        return ok();
    }
}
