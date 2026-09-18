package com.ingot.cloud.bff.web;

import java.util.Map;

import com.ingot.cloud.bff.model.dto.BffCompleteDTO;
import com.ingot.cloud.bff.model.dto.BffLoginDTO;
import com.ingot.cloud.bff.model.dto.BffTenantSelectDTO;
import com.ingot.cloud.bff.service.BffAuthService;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.crypto.annotation.InCryptoHybridContext;
import com.ingot.framework.security.crypto.annotation.InDecrypt;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 双入口 BFF 认证 API。浏览器不传 domain 或重定向 URL。
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/bff/auth")
@RequiredArgsConstructor
public class BffAuthAPI implements RShortcuts {
    private final BffAuthService authService;

    @Permit
    @PostMapping("/csrf")
    public R<Map<String, Object>> csrf(HttpServletRequest request, HttpServletResponse response) {
        return ok(authService.issueCsrf(request, response));
    }

    @Permit
    @PostMapping("/platform/transactions")
    public R<Map<String, Object>> createPlatformTransaction(HttpServletRequest request, HttpServletResponse response) {
        return ok(authService.createTransaction(BffConstants.ENTRY_PLATFORM, request, response));
    }

    @Permit
    @PostMapping("/tenant/transactions")
    public R<Map<String, Object>> createTenantTransaction(HttpServletRequest request, HttpServletResponse response) {
        return ok(authService.createTransaction(BffConstants.ENTRY_TENANT, request, response));
    }

    @Permit
    @GetMapping("/platform/transactions/{id}")
    public R<Map<String, Object>> readPlatformTransaction(@PathVariable("id") String id,
            HttpServletRequest request) {
        return ok(authService.readTransaction(BffConstants.ENTRY_PLATFORM, id, request));
    }

    @Permit
    @GetMapping("/tenant/transactions/{id}")
    public R<Map<String, Object>> readTenantTransaction(@PathVariable("id") String id,
            HttpServletRequest request) {
        return ok(authService.readTransaction(BffConstants.ENTRY_TENANT, id, request));
    }

    @Permit
    @InCryptoHybridContext
    @InDecrypt
    @PostMapping("/platform/login")
    public R<?> platformLogin(@RequestBody BffLoginDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {
        return authService.login(BffConstants.ENTRY_PLATFORM, dto, request, response);
    }

    @Permit
    @InCryptoHybridContext
    @InDecrypt
    @PostMapping("/tenant/login")
    public R<?> tenantLogin(@RequestBody BffLoginDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {
        return authService.login(BffConstants.ENTRY_TENANT, dto, request, response);
    }

    @Permit
    @PostMapping("/tenant/select")
    public R<?> selectTenant(@RequestBody BffTenantSelectDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {
        return authService.selectTenant(dto.getTenantId(), dto.getTransactionId(), request, response);
    }

    @Permit
    @PostMapping("/platform/complete")
    public R<Map<String, Object>> completePlatform(@RequestBody BffCompleteDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {
        return ok(authService.complete(BffConstants.ENTRY_PLATFORM, dto.getTicket(), request, response));
    }

    @Permit
    @PostMapping("/tenant/complete")
    public R<Map<String, Object>> completeTenant(@RequestBody BffCompleteDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {
        return ok(authService.complete(BffConstants.ENTRY_TENANT, dto.getTicket(), request, response));
    }

    @Permit
    @DeleteMapping("/logout")
    public R<?> logout(HttpServletRequest request, HttpServletResponse response) {
        return authService.logout(request, response);
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me(HttpServletRequest request) {
        return authService.me(request);
    }
}
