package com.ingot.cloud.pms.web.v1.auth;

import java.util.List;

import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizAuthService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : AuthAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 15:05.</p>
 */
@RestController
@Tag(description = "AuthUser", name = "权限流程接口模块")
@RequestMapping(value = "/v1/auth/user")
@RequiredArgsConstructor
public class AuthUserAPI implements RShortcuts {
    private final BizAuthService bizAuthService;

    @GetMapping(value = "/info")
    @Operation(summary = "用户信息", description = "获取当前用户信息")
    public R<UserInfoDTO> getUserInfo() {
        return ok(bizAuthService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @GetMapping(value = "/menus")
    @Operation(summary = "用户菜单", description = "获取当前用户菜单")
    public R<List<MenuTreeNodeVO>> getUserMenus() {
        return ok(bizAuthService.getUserMenus(SecurityAuthContext.getUser()));
    }
}
