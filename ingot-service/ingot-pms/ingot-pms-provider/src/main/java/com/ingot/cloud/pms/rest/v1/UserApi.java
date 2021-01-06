package com.ingot.cloud.pms.rest.v1;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.service.SysUserService;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : UserApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/11.</p>
 * <p>Time         : 6:48 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/user")
@AllArgsConstructor
public class UserApi extends BaseController {
    private final SysUserService sysUserService;

    @GetMapping
    public IngotResponse<?> user() {
        return ok(sysUserService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @GetMapping("/page")
    public IngotResponse<?> page(Page<?> page, SysUser user) {
        return ok();
    }
}
