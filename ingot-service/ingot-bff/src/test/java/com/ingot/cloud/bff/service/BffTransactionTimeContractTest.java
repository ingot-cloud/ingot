package com.ingot.cloud.bff.service;

import java.time.Instant;
import java.util.TimeZone;

import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>BFF 对外事务截止为 ISO，内部秒时间戳及登录绑定保持原语义。</p>
 * @author jy
 * @since 1.0.0
 */
class BffTransactionTimeContractTest {
    @Test
    void transactionViewEmitsUtcWithoutChangingStoredEpochSeconds() throws Exception {
        var transactions = mock(LoginTransactionService.class);
        var sessions = mock(BffSessionService.class);
        var registry = mock(BffAppRegistry.class);
        var request = mock(HttpServletRequest.class);
        var app = new BffAppRegistration();
        app.setAppId("tenant-admin");
        app.setDomain(AuthorizationDomain.TENANT);
        when(registry.requireFromRequest(any())).thenReturn(app);
        when(sessions.getBindingIdFromCookie(request)).thenReturn("binding");
        var binding = new AuthBinding();
        binding.setAppId(app.getAppId());
        binding.setBindingId("binding");
        when(transactions.requireBinding("binding")).thenReturn(binding);
        var transaction = new LoginTransaction();
        transaction.setTransactionId("transaction");
        transaction.setAppId(app.getAppId());
        transaction.setStage("LOGIN");
        transaction.setLoginBindingId("binding");
        long expiresAt = Instant.parse("2030-01-02T03:04:05Z").getEpochSecond();
        transaction.setExpiresAt(expiresAt);
        var schema = io.swagger.v3.core.converter.ModelConverters.getInstance()
                .read(com.ingot.cloud.bff.model.dto.BffTransactionView.class).get("BffTransactionView");
        var expiresAtSchema = (io.swagger.v3.oas.models.media.Schema<?>) schema.getProperties().get("expiresAt");
        assertEquals("string", expiresAtSchema.getType());
        assertEquals("date-time", expiresAtSchema.getFormat());
        when(transactions.require("transaction")).thenReturn(transaction);
        var service = new BffAuthService(new BffProperties(), registry, sessions, transactions,
                mock(RemoteAuthTokenService.class), mock(RemoteAuthSessionService.class),
                mock(AccountLockSignalPort.class), new AccountLockBffProperties());
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                var view = service.readTransaction(BffConstants.ENTRY_TENANT, "transaction", request);
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                        .registerModule(new com.ingot.framework.commons.jackson.InApiTimeModule());
                var json = mapper.readTree(mapper.writeValueAsBytes(view));
                assertEquals("2030-01-02T03:04:05Z", json.get("expiresAt").textValue());
                assertEquals("transaction", json.get("transactionId").textValue());
                assertEquals("LOGIN", json.get("stage").textValue());
                assertEquals(false, json.has("allows"));
                assertEquals(false, json.has("completionUrl"));
                assertEquals(expiresAt, transaction.getExpiresAt());
            }
        } finally {
            TimeZone.setDefault(original);
        }
        verify(transactions, never()).save(any());
    }
}
