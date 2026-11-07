package com.ingot.cloud.iam.interfaces.http;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.application.diagnose.AuthorizationAuditQuery;
import com.ingot.cloud.iam.application.diagnose.AuthorizationAuditService;
import com.ingot.cloud.iam.application.diagnose.DiagnoseAuditService;
import com.ingot.cloud.iam.application.diagnose.TenantDiagnoseCandidates;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformAuthorizationAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantAuthorizationAPI;
import com.ingot.framework.commons.jackson.ApiTimeFormatterRegistrar;
import com.ingot.cloud.iam.application.audit.model.AuditCursorPage;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.core.error.GlobalExceptionHandlerResolver;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <p>验证两域审计 HTTP 参数使用游标与框架严格时间契约，非法筛选在进入查询前返回 400。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AuthorizationAuditAPITest {
    private static final AuditCursorPage EMPTY = new AuditCursorPage(List.of(), null, false, Map.of());

    @Test
    void optionalFiltersCursorAndOffsetTimeReachBothDomains() throws Exception {
        var service = mock(AuthorizationAuditService.class);
        when(service.list(any(), anyInt(), any(), any())).thenReturn(EMPTY);
        var conversion = new DefaultFormattingConversionService();
        new ApiTimeFormatterRegistrar().registerFormatters(conversion);
        var diagnoses = mock(DiagnoseAuditService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PlatformAuthorizationAPI(diagnoses, service),
                        new TenantAuthorizationAPI(diagnoses, service, mock(TenantDiagnoseCandidates.class)))
                .setConversionService(conversion)
                .setControllerAdvice(new IamErrorHandler(), new GlobalExceptionHandlerResolver()).build();
        for (var domain : AuthorizationDomain.values()) {
            String path = "/v1/" + domain.name().toLowerCase(java.util.Locale.ROOT) + "/authorization/audits";
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.data.hasMore").value(false))
                    .andExpect(jsonPath("$.data.total").doesNotExist());
            verify(service).list(domain, AuthorizationAuditService.DEFAULT_LIMIT, null,
                    new AuthorizationAuditQuery(null, null, null, null, null, null));
            mvc.perform(get(path).param("limit", "50").param("cursor", "abc")
                            .param("from", "2026-10-11T09:00:00+08:00").param("until", "2026-10-11T02:00:00Z")
                            .param("actorMemberId", "101").param("targetType", " resource ").param("targetId", "88"))
                    .andExpect(status().isOk());
            verify(service).list(domain, 50, "abc", new AuthorizationAuditQuery("101", "resource", "88", null,
                    Instant.parse("2026-10-11T01:00:00Z"), Instant.parse("2026-10-11T02:00:00Z")));
            reset(service);
            when(service.list(any(), anyInt(), any(), any())).thenReturn(EMPTY);
            for (String invalid : List.of("2026-10-11T09:00:00", "2026-10-11 09:00:00", "invalid")) {
                mvc.perform(get(path).param("from", invalid)).andExpect(status().isBadRequest());
            }
            mvc.perform(get(path).param("changeType", "UNKNOWN")).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("InvalidArgument"));
            mvc.perform(get(path).param("actorMemberId", "-1")).andExpect(status().isBadRequest());
            mvc.perform(get(path).param("from", "2026-10-11T02:00:00Z").param("until", "2026-10-11T01:00:00Z"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }
    }
}
