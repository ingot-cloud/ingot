package com.ingot.cloud.pms.web.v1.app;

import com.ingot.cloud.pms.api.model.dto.auth.MiniProgramRegisterDTO;
import com.ingot.cloud.pms.service.biz.LoginService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : AppAuthAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/18.</p>
 * <p>Time         : 17:33.</p>
 */
@Slf4j
@Tag(description = "AppAuth", name = "App授权模块")
@RestController
@RequestMapping(value = "/v1/app/auth")
@RequiredArgsConstructor
public class AppAuthAPI implements RShortcuts {
    private final LoginService loginService;

    @Operation(summary = "小程序注册")
    @Permit
    @PostMapping("/register/social")
    public R<?> register(@RequestBody MiniProgramRegisterDTO params) {
        loginService.appMiniProgramRegister(params);
        return ok();
    }
}
