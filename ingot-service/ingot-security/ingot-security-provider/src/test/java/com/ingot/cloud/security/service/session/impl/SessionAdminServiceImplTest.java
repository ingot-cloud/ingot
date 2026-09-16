package com.ingot.cloud.security.service.session.impl;

import java.time.Instant;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ingot.cloud.auth.api.model.dto.InnerSessionQueryDTO;
import com.ingot.cloud.auth.api.model.dto.InnerSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.dto.InnerUserSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionPageVO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.iam.api.model.domain.SysTenant;
import com.ingot.cloud.iam.api.model.dto.user.InnerUserDTO;
import com.ingot.cloud.iam.api.rpc.RemoteIamTenantDetailsService;
import com.ingot.cloud.iam.api.rpc.RemoteIamUserDetailsService;
import com.ingot.cloud.security.api.model.dto.session.PlatformSessionQueryDTO;
import com.ingot.cloud.security.api.model.dto.session.PlatformUserSessionRevokeDTO;
import com.ingot.cloud.security.api.model.vo.session.PlatformSessionVO;
import com.ingot.cloud.security.model.convert.PlatformSessionConvertImpl;
import com.ingot.framework.commons.error.IllegalOperationException;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.core.context.InMessageSource;
import com.ingot.framework.core.utils.validation.DefaultAssertionChecker;
import com.ingot.framework.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SessionAdminServiceImpl} 会话管理面单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class SessionAdminServiceImplTest {

    private static final Long TENANT_ID = 1L;
    private static final String CLIENT_ID = "web";
    private static final Long USER_ID = 9L;
    private static final Long ACTOR_ID = 100L;
    private static final String SID = "session-1";

    private final RemoteAuthSessionService remoteAuthSessionService = mock(RemoteAuthSessionService.class);
    private final RemoteIamUserDetailsService remoteIamUserDetailsService = mock(RemoteIamUserDetailsService.class);
    private final RemoteIamTenantDetailsService remoteIamTenantDetailsService =
            mock(RemoteIamTenantDetailsService.class);

    private final SessionAdminServiceImpl service = new SessionAdminServiceImpl(
            remoteAuthSessionService, remoteIamUserDetailsService, remoteIamTenantDetailsService,
            new PlatformSessionConvertImpl(), new DefaultAssertionChecker(messageSource()));

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void page_withoutClientIdAndUserId_isRejected() {
        PlatformSessionQueryDTO params = new PlatformSessionQueryDTO();
        params.setTenantId(TENANT_ID);

        assertThrows(IllegalOperationException.class, () -> service.page(params));
    }

    @Test
    void page_withoutTenantIdAndContext_isRejected() {
        PlatformSessionQueryDTO params = new PlatformSessionQueryDTO();
        params.setClientId(CLIENT_ID);

        assertThrows(IllegalOperationException.class, () -> service.page(params));
    }

    @Test
    void page_withoutTenantId_fallsBackToTenantContext() {
        TenantContextHolder.set(TENANT_ID);
        PlatformSessionQueryDTO params = new PlatformSessionQueryDTO();
        params.setClientId(CLIENT_ID);
        givenInnerPage(1L, List.of());
        givenTenant("平台租户");

        service.page(params);

        ArgumentCaptor<InnerSessionQueryDTO> captor = ArgumentCaptor.forClass(InnerSessionQueryDTO.class);
        verify(remoteAuthSessionService).page(captor.capture());
        assertEquals(TENANT_ID, captor.getValue().getTenantId());
    }

    @Test
    void page_withClientId_keepsInnerTotalAndFillsNames() {
        givenInnerPage(30L, List.of(session(SID, UserTypeEnum.ADMIN)));
        givenTenant("平台租户");
        givenAdminUser("admin", "超级管理员");

        IPage<PlatformSessionVO> page = service.page(query());

        assertEquals(30L, page.getTotal());
        PlatformSessionVO record = page.getRecords().get(0);
        assertEquals(SID, record.getSid());
        assertEquals("admin", record.getUsername());
        assertEquals("超级管理员", record.getNickname());
        assertEquals("平台租户", record.getTenantName());
    }

    @Test
    void page_iamUnavailable_keepsSessionFactsWithoutNames() {
        givenInnerPage(1L, List.of(session(SID, UserTypeEnum.ADMIN)));
        when(remoteIamTenantDetailsService.getTenantById(anyLong())).thenThrow(new IllegalStateException("IAM down"));
        when(remoteIamUserDetailsService.getAllUserInfo(anyList())).thenThrow(new IllegalStateException("IAM down"));

        PlatformSessionVO record = service.page(query()).getRecords().get(0);

        assertEquals(SID, record.getSid());
        // IAM 不可用只丢名称，登录账号名回落会话内的 principalName，管理员仍能按 sid 下线
        assertEquals("principal", record.getUsername());
        assertNull(record.getNickname());
        assertNull(record.getTenantName());
    }

    @Test
    void page_appUserSession_skipsIamUserLookup() {
        givenInnerPage(1L, List.of(session(SID, UserTypeEnum.APP)));
        givenTenant("平台租户");

        PlatformSessionVO record = service.page(query()).getRecords().get(0);

        assertNull(record.getNickname());
        verify(remoteIamUserDetailsService, never()).getAllUserInfo(anyList());
    }

    @Test
    void page_userIdOnly_queriesAcrossClientsAndSlicesWithExactTotal() {
        PlatformSessionQueryDTO params = new PlatformSessionQueryDTO();
        params.setTenantId(TENANT_ID);
        params.setUserId(USER_ID);
        params.setSize(1L);
        InnerSessionVO older = session("s1", UserTypeEnum.ADMIN);
        older.setIssuedAt(Instant.parse("2026-08-01T00:00:00Z"));
        InnerSessionVO newer = session("s2", UserTypeEnum.ADMIN);
        newer.setIssuedAt(Instant.parse("2026-08-02T00:00:00Z"));
        when(remoteAuthSessionService.listByUser(any())).thenReturn(R.ok(List.of(older, newer)));
        givenTenant("平台租户");
        givenAdminUser("admin", "超级管理员");

        IPage<PlatformSessionVO> page = service.page(params);

        assertEquals(2L, page.getTotal());
        // 创建时间倒序，第一页取最新的一条
        assertEquals(List.of("s2"), page.getRecords().stream().map(PlatformSessionVO::getSid).toList());
        ArgumentCaptor<InnerSessionQueryDTO> captor = ArgumentCaptor.forClass(InnerSessionQueryDTO.class);
        verify(remoteAuthSessionService).listByUser(captor.capture());
        // clientId 留空表示跨 Client
        assertNull(captor.getValue().getClientId());
        verify(remoteAuthSessionService, never()).page(any());
    }

    @Test
    void getBySid_miss_returnsNull() {
        when(remoteAuthSessionService.getBySid(SID)).thenReturn(R.ok(null));

        assertNull(service.getBySid(SID));
    }

    @Test
    void revokeBySid_forcesAdminReasonAndCurrentActor() {
        when(remoteAuthSessionService.revokeBySid(isNull(), eq(SID), any())).thenReturn(R.ok(true));

        assertTrue(service.revokeBySid(SID, ACTOR_ID));

        ArgumentCaptor<InnerSessionRevokeDTO> captor = ArgumentCaptor.forClass(InnerSessionRevokeDTO.class);
        verify(remoteAuthSessionService).revokeBySid(isNull(), eq(SID), captor.capture());
        assertEquals(SessionRevokeReason.ADMIN_REVOKE, captor.getValue().getReason());
        assertEquals(ACTOR_ID, captor.getValue().getActorId());
    }

    @Test
    void revokeByUser_withoutUserId_isRejected() {
        PlatformUserSessionRevokeDTO params = new PlatformUserSessionRevokeDTO();
        params.setTenantId(TENANT_ID);

        assertThrows(IllegalOperationException.class, () -> service.revokeByUser(params, ACTOR_ID));
    }

    @Test
    void revokeByUser_blankClientId_revokesAcrossClients() {
        PlatformUserSessionRevokeDTO params = new PlatformUserSessionRevokeDTO();
        params.setTenantId(TENANT_ID);
        params.setUserId(USER_ID);
        when(remoteAuthSessionService.revokeByUser(any())).thenReturn(R.ok(2));

        assertEquals(2, service.revokeByUser(params, ACTOR_ID));

        ArgumentCaptor<InnerUserSessionRevokeDTO> captor = ArgumentCaptor.forClass(InnerUserSessionRevokeDTO.class);
        verify(remoteAuthSessionService).revokeByUser(captor.capture());
        assertNull(captor.getValue().getClientId());
        assertEquals(SessionRevokeReason.ADMIN_REVOKE, captor.getValue().getReason());
        assertEquals(ACTOR_ID, captor.getValue().getActorId());
    }

    private void givenInnerPage(long total, List<InnerSessionVO> records) {
        when(remoteAuthSessionService.page(any())).thenReturn(R.ok(InnerSessionPageVO.builder()
                .current(1L)
                .size(20L)
                .total(total)
                .records(records)
                .build()));
    }

    private void givenTenant(String name) {
        SysTenant tenant = new SysTenant();
        tenant.setId(TENANT_ID);
        tenant.setName(name);
        when(remoteIamTenantDetailsService.getTenantById(TENANT_ID)).thenReturn(R.ok(tenant));
    }

    private void givenAdminUser(String username, String nickname) {
        InnerUserDTO user = new InnerUserDTO();
        user.setId(USER_ID);
        user.setUsername(username);
        user.setNickname(nickname);
        when(remoteIamUserDetailsService.getAllUserInfo(anyList())).thenReturn(R.ok(List.of(user)));
    }

    private static PlatformSessionQueryDTO query() {
        PlatformSessionQueryDTO params = new PlatformSessionQueryDTO();
        params.setTenantId(TENANT_ID);
        params.setClientId(CLIENT_ID);
        return params;
    }

    private static InnerSessionVO session(String sid, UserTypeEnum userType) {
        return InnerSessionVO.builder()
                .sid(sid)
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .clientId(CLIENT_ID)
                .principalName("principal")
                .userType(userType.getValue())
                .build();
    }

    /**
     * 无国际化资源时回落默认文案，断言只关心是否拒绝而非文案内容。
     */
    private static InMessageSource messageSource() {
        InMessageSource messageSource = mock(InMessageSource.class);
        when(messageSource.getMessage(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(1));
        return messageSource;
    }
}
