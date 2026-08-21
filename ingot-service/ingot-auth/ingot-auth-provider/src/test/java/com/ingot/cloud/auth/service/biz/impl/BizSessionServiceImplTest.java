package com.ingot.cloud.auth.service.biz.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.ingot.cloud.auth.api.model.dto.InnerSessionQueryDTO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionPageVO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.cloud.auth.model.convert.SessionConvertImpl;
import com.ingot.framework.commons.error.IllegalOperationException;
import com.ingot.framework.core.context.InMessageSource;
import com.ingot.framework.core.utils.validation.DefaultAssertionChecker;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BizSessionServiceImpl} 会话查询编排单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class BizSessionServiceImplTest {

    private static final Long TENANT_ID = 1L;
    private static final String CLIENT_ID = "web";
    private static final Long USER_ID = 9L;

    private final OnlineTokenService onlineTokenService = mock(OnlineTokenService.class);
    private final BizSessionServiceImpl service = new BizSessionServiceImpl(
            onlineTokenService, new SessionConvertImpl(), new DefaultAssertionChecker(messageSource()));

    @Test
    void page_withoutTenantOrClient_isRejected() {
        InnerSessionQueryDTO params = new InnerSessionQueryDTO();
        params.setTenantId(TENANT_ID);

        assertThrows(IllegalOperationException.class, () -> service.page(params));
    }

    @Test
    void page_withoutFilter_pagesByOnlineUsers() {
        InnerSessionQueryDTO params = query();
        params.setCurrent(2L);
        params.setSize(10L);
        when(onlineTokenService.getOnlineUserCount(TENANT_ID, CLIENT_ID)).thenReturn(30L);
        when(onlineTokenService.getOnlineUsers(TENANT_ID, CLIENT_ID, 10L, 10L)).thenReturn(List.of(USER_ID));
        when(onlineTokenService.listUserSessions(TENANT_ID, CLIENT_ID, List.of(USER_ID)))
                .thenReturn(List.of(session("s1", USER_ID, null)));

        InnerSessionPageVO page = service.page(params);

        assertEquals(2L, page.getCurrent());
        assertEquals(30L, page.getTotal());
        assertEquals(List.of("s1"), page.getRecords().stream().map(InnerSessionVO::getSid).toList());
    }

    @Test
    void page_noOnlineUser_returnsEmptyWithoutExpanding() {
        when(onlineTokenService.getOnlineUserCount(TENANT_ID, CLIENT_ID)).thenReturn(0L);

        InnerSessionPageVO page = service.page(query());

        assertEquals(0L, page.getTotal());
        verify(onlineTokenService, never()).getOnlineUsers(TENANT_ID, CLIENT_ID, 0L, 20L);
    }

    @Test
    void page_byUserId_slicesInMemoryWithExactTotal() {
        InnerSessionQueryDTO params = query();
        params.setUserId(USER_ID);
        params.setSize(1L);
        when(onlineTokenService.listSids(TENANT_ID, CLIENT_ID, USER_ID)).thenReturn(List.of("s1", "s2"));
        when(onlineTokenService.getBySid("s1"))
                .thenReturn(Optional.of(session("s1", USER_ID, Instant.parse("2026-08-01T00:00:00Z"))));
        when(onlineTokenService.getBySid("s2"))
                .thenReturn(Optional.of(session("s2", USER_ID, Instant.parse("2026-08-02T00:00:00Z"))));

        InnerSessionPageVO page = service.page(params);

        assertEquals(2L, page.getTotal());
        // 创建时间倒序，第一页取最新的一条
        assertEquals(List.of("s2"), page.getRecords().stream().map(InnerSessionVO::getSid).toList());
    }

    @Test
    void page_byIp_filtersOtherClientsAndUsers() {
        InnerSessionQueryDTO params = query();
        params.setIpAddress("10.0.0.1");
        when(onlineTokenService.listSidsByIp(TENANT_ID, "10.0.0.1")).thenReturn(List.of("s1", "s2"));
        when(onlineTokenService.getBySid("s1")).thenReturn(Optional.of(session("s1", USER_ID, null)));
        OnlineToken otherClient = session("s2", USER_ID, null);
        otherClient.setClientId("app");
        when(onlineTokenService.getBySid("s2")).thenReturn(Optional.of(otherClient));

        InnerSessionPageVO page = service.page(params);

        assertEquals(1L, page.getTotal());
        assertEquals("s1", page.getRecords().get(0).getSid());
    }

    @Test
    void listByUser_blankClientId_queriesAcrossClients() {
        InnerSessionQueryDTO params = new InnerSessionQueryDTO();
        params.setTenantId(TENANT_ID);
        params.setUserId(USER_ID);
        when(onlineTokenService.listUserSessions(TENANT_ID, USER_ID))
                .thenReturn(List.of(session("s1", USER_ID, null)));

        assertEquals(1, service.listByUser(params).size());
        verify(onlineTokenService, never()).listUserSessions(TENANT_ID, CLIENT_ID, USER_ID);
    }

    @Test
    void listByUser_withoutUserId_isRejected() {
        InnerSessionQueryDTO params = new InnerSessionQueryDTO();
        params.setTenantId(TENANT_ID);

        assertThrows(IllegalOperationException.class, () -> service.listByUser(params));
    }

    @Test
    void getBySid_miss_returnsNull() {
        when(onlineTokenService.getBySid("missing")).thenReturn(Optional.empty());

        assertNull(service.getBySid("missing"));
    }

    /**
     * 无国际化资源时回落默认文案，断言只关心是否拒绝而非文案内容。
     */
    private static InMessageSource messageSource() {
        InMessageSource messageSource = mock(InMessageSource.class);
        when(messageSource.getMessage(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(1));
        return messageSource;
    }

    private static InnerSessionQueryDTO query() {
        InnerSessionQueryDTO params = new InnerSessionQueryDTO();
        params.setTenantId(TENANT_ID);
        params.setClientId(CLIENT_ID);
        return params;
    }

    private static OnlineToken session(String sid, Long userId, Instant issuedAt) {
        return OnlineToken.builder()
                .sid(sid)
                .userId(userId)
                .tenantId(TENANT_ID)
                .clientId(CLIENT_ID)
                .principalName("admin")
                .issuedAt(issuedAt)
                .build();
    }
}
