package com.ingot.cloud.iam.web.v1;

import java.time.Instant;
import java.util.List;

import com.ingot.cloud.iam.diagnose.AuthorizationAuditQuery;
import com.ingot.cloud.iam.diagnose.DiagnoseAuditService;
import com.ingot.cloud.iam.web.IamErrorHandler;
import com.ingot.cloud.iam.web.v1.platform.PlatformAuthorizationAPI;
import com.ingot.cloud.iam.web.v1.tenant.TenantAuthorizationAPI;
import com.ingot.framework.commons.jackson.ApiTimeFormatterRegistrar;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.core.error.GlobalExceptionHandlerResolver;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <p>验证两域审计 HTTP 参数使用框架严格时间契约，非法筛选在进入查询前返回 400。</p>
 * @author jy
 * @since 1.0.0
 */
class AuthorizationAuditAPITest {
    @Test
    void optionalFiltersAndOffsetTimeReachBothDomains() throws Exception {
        var service = mock(DiagnoseAuditService.class);
        when(service.listAudits(any(), anyInt(), anyInt(), any())).thenReturn(new PageResponse<>(List.of(), 0L, 1, 20));
        var conversion = new DefaultFormattingConversionService();
        new ApiTimeFormatterRegistrar().registerFormatters(conversion);
        var mvc = MockMvcBuilders.standaloneSetup(new PlatformAuthorizationAPI(service), new TenantAuthorizationAPI(service))
                .setConversionService(conversion)
                .setControllerAdvice(new IamErrorHandler(), new GlobalExceptionHandlerResolver()).build();
        for (var domain : AuthorizationDomain.values()) {
            String path = "/v1/" + domain.name().toLowerCase(java.util.Locale.ROOT) + "/authorization/audits";
            mvc.perform(get(path)).andExpect(status().isOk());
            verify(service).listAudits(domain, 1, 20, new AuthorizationAuditQuery(null, null, null, null, null, null));
            mvc.perform(get(path).param("from", "2026-10-11T09:00:00+08:00").param("until", "2026-10-11T02:00:00Z")
                            .param("actorMemberId", "101").param("targetType", " resource ").param("targetId", "88"))
                    .andExpect(status().isOk());
            verify(service).listAudits(domain, 1, 20, new AuthorizationAuditQuery("101", "resource", "88", null,
                    Instant.parse("2026-10-11T01:00:00Z"), Instant.parse("2026-10-11T02:00:00Z")));
            reset(service);
            when(service.listAudits(any(), anyInt(), anyInt(), any())).thenReturn(new PageResponse<>(List.of(), 0L, 1, 20));
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
