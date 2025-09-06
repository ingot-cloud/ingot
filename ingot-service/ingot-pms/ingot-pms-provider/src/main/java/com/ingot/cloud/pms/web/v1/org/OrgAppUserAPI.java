package com.ingot.cloud.pms.web.v1.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.service.biz.BizAppUserService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(description = "orgAppUser", name = "组织App用户模块")
@RestController
@RequestMapping(value = "/v1/org/appUser")
@RequiredArgsConstructor
public class OrgAppUserAPI implements RShortcuts {
    private final BizAppUserService bizAppUserService;

    @GetMapping("/page")
    public R<?> page(Page<AppUser> page, AppUser condition) {
        return ok(bizAppUserService.pageTenant(page, condition));
    }

    @PutMapping
    public R<?> update(@RequestBody AppUser params) {
        bizAppUserService.updateUser(params);
        return ok();
    }
}
