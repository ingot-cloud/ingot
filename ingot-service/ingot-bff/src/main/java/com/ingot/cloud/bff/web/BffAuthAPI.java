package com.ingot.cloud.bff.web;

import java.util.Map;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.cloud.bff.model.dto.BffLoginDTO;
import com.ingot.cloud.bff.model.dto.BffTenantSelectDTO;
import com.ingot.cloud.bff.model.dto.BffTransactionView;
import com.ingot.cloud.bff.service.BffAuthService;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.crypto.annotation.InCryptoHybridContext;
import com.ingot.framework.security.crypto.annotation.InDecrypt;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 同源双入口认证 API；login/select 直接建立当前应用正式会话。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController

@RequiredArgsConstructor
public class BffAuthAPI implements RShortcuts {

    private final BffAuthService auth;

    /** 同源匿名自举；只接受空 JSON。 */
    @Permit
    @PostMapping(BffConstants.CSRF)
    public R<Map<String, Object>> csrf(@RequestBody Map<String, Object> body, HttpServletRequest request,
            HttpServletResponse response) {
        empty(body);
        return ok(auth.issueCsrf(request, response));
    }

    /** 平台用户点击时准备事务。 */
    @Permit
    @PostMapping(BffConstants.PLATFORM_TRANSACTIONS)
    public R<BffTransactionView> createPlatform(@RequestBody Map<String, Object> body, HttpServletRequest request,
            HttpServletResponse response) {
        empty(body);
        return ok(auth.createTransaction(BffConstants.ENTRY_PLATFORM, request, response));
    }

    /** 租户用户点击时准备事务。 */
    @Permit
    @PostMapping(BffConstants.TENANT_TRANSACTIONS)
    public R<BffTransactionView> createTenant(@RequestBody Map<String, Object> body, HttpServletRequest request,
            HttpServletResponse response) {
        empty(body);
        return ok(auth.createTransaction(BffConstants.ENTRY_TENANT, request, response));
    }

    /** 只读本绑定的平台事务。 */
    @Permit
    @GetMapping(BffConstants.PLATFORM_TRANSACTION)
    public R<BffTransactionView> readPlatform(@PathVariable(BffConstants.TRANSACTION_VARIABLE) String id,
            HttpServletRequest request) {
        return ok(auth.readTransaction(BffConstants.ENTRY_PLATFORM, id, request));
    }

    /** 只读本绑定的租户事务。 */
    @Permit
    @GetMapping(BffConstants.TENANT_TRANSACTION)
    public R<BffTransactionView> readTenant(@PathVariable(BffConstants.TRANSACTION_VARIABLE) String id,
            HttpServletRequest request) {
        return ok(auth.readTransaction(BffConstants.ENTRY_TENANT, id, request));
    }

    /** 原绑定有界恢复平台结果，不再执行认证。 */
    @Permit
    @PostMapping(BffConstants.PLATFORM_RESUME)
    public R<BffTransactionView> resumePlatform(@PathVariable(BffConstants.TRANSACTION_VARIABLE) String id,
            @RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) {
        empty(body);
        return ok(auth.resume(BffConstants.ENTRY_PLATFORM, id, request, response));
    }

    /** 原绑定有界恢复租户结果，不再执行认证。 */
    @Permit
    @PostMapping(BffConstants.TENANT_RESUME)
    public R<BffTransactionView> resumeTenant(@PathVariable(BffConstants.TRANSACTION_VARIABLE) String id,
            @RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) {
        empty(body);
        return ok(auth.resume(BffConstants.ENTRY_TENANT, id, request, response));
    }

    /** 整包解密的平台认证，无租户选择。 */
    @Permit
    @InCryptoHybridContext
    @InDecrypt
    @PostMapping(BffConstants.PLATFORM_LOGIN)
    public R<?> platformLogin(@RequestBody BffLoginDTO body, HttpServletRequest request, HttpServletResponse response) {
        return auth.login(BffConstants.ENTRY_PLATFORM, body, request, response);
    }

    /** 整包解密的租户认证，多候选留在本页选择。 */
    @Permit
    @InCryptoHybridContext
    @InDecrypt
    @PostMapping(BffConstants.TENANT_LOGIN)
    public R<?> tenantLogin(@RequestBody BffLoginDTO body, HttpServletRequest request, HttpServletResponse response) {
        return auth.login(BffConstants.ENTRY_TENANT, body, request, response);
    }

    /** 选定租户后直接建立本域正式会话。 */
    @Permit
    @PostMapping(BffConstants.TENANT_SELECT)
    public R<?> select(@RequestBody BffTenantSelectDTO body, HttpServletRequest request, HttpServletResponse response) {
        return auth.selectTenant(body.getTenantId(), body.getTransactionId(), request, response);
    }

    /** 同源退出，当前应用 Cookie 清理兜底。 */
    @Permit
    @DeleteMapping(BffConstants.LOGOUT)
    public R<?> logout(HttpServletRequest request, HttpServletResponse response) {
        return auth.logout(request, response);
    }

    /** 用实际正式 Cookie 确认本应用身份。 */
    @Permit
    @GetMapping(BffConstants.ME)
    public org.springframework.http.ResponseEntity<R<Map<String, Object>>> me(HttpServletRequest request) {
        R<Map<String, Object>> result = auth.me(request);
        return org.springframework.http.ResponseEntity.status(result.isSuccess()
                ? org.springframework.http.HttpStatus.OK : org.springframework.http.HttpStatus.UNAUTHORIZED)
            .body(result);
    }

    private void empty(Map<String, Object> body) {
        if (body == null || !body.isEmpty())
            throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
    }

}
