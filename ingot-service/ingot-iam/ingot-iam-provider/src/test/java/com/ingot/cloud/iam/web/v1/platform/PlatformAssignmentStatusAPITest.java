package com.ingot.cloud.iam.web.v1.platform;

import java.util.List;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.assignment.PlatformAuthorizationEditor;
import com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus;
import com.ingot.framework.commons.model.iam.AuthorizationCandidateKind;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.SubjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>验证平台分配列表的计算状态查询绑定、未知值拒绝和旧请求兼容。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class PlatformAssignmentStatusAPITest {

    @Test
    void upgradeObjectsBindTheSharedPickerRevisionQuery() throws Exception {
        var editor = mock(PlatformAuthorizationEditor.class);
        when(editor.upgradeCandidates("1004701", AuthorizationCandidateKind.OBJECT, "1003602", "applications",
                null, List.of(), 1, 20, false, null))
                .thenReturn(new AuthorizationCandidatePage(List.of(), 0, 1, 20, true, null));
        MockMvcBuilders.standaloneSetup(new PlatformAssignmentAPI(mock(AssignmentService.class), editor)).build()
                .perform(get("/v1/platform/assignments/upgrade/candidates")
                        .param("assignmentId", "1004701").param("kind", "OBJECT")
                        .param("revisionId", "1003602").param("parameterKey", "applications"))
                .andExpect(status().isOk());
        verify(editor).upgradeCandidates("1004701", AuthorizationCandidateKind.OBJECT, "1003602", "applications",
                null, List.of(), 1, 20, false, null);
    }

    @ParameterizedTest
    @EnumSource(AssignmentEffectiveStatus.class)
    void bindsEachComputedStateAlongsideExistingFilters(AssignmentEffectiveStatus state) throws Exception {
        AssignmentService service = mock(AssignmentService.class);
        when(service.list(AuthorizationDomain.PLATFORM, 2, 20, SubjectType.GROUP, "运维", state))
                .thenReturn(new PageResponse<>(List.of(), 0, 2, 20));
        MockMvcBuilders.standaloneSetup(new PlatformAssignmentAPI(service, org.mockito.Mockito.mock(com.ingot.cloud.iam.assignment.PlatformAuthorizationEditor.class))).build()
                .perform(get("/v1/platform/assignments").param("page", "2").param("pageSize", "20")
                        .param("subjectType", "GROUP").param("keyword", "运维").param("effectiveStatus", state.getValue()))
                .andExpect(status().isOk());
        verify(service).list(AuthorizationDomain.PLATFORM, 2, 20, SubjectType.GROUP, "运维", state);
    }

    @Test
    void omittedStatePreservesTheDefaultList() throws Exception {
        AssignmentService service = mock(AssignmentService.class);
        when(service.list(AuthorizationDomain.PLATFORM, 1, 20, null, null, null))
                .thenReturn(new PageResponse<>(List.of(), 0, 1, 20));
        MockMvcBuilders.standaloneSetup(new PlatformAssignmentAPI(service, org.mockito.Mockito.mock(com.ingot.cloud.iam.assignment.PlatformAuthorizationEditor.class))).build()
                .perform(get("/v1/platform/assignments")).andExpect(status().isOk());
        verify(service).list(AuthorizationDomain.PLATFORM, 1, 20, null, null, null);
    }

    @Test
    void unknownStateIsRejectedBeforeTheListService() throws Exception {
        AssignmentService service = mock(AssignmentService.class);
        MockMvcBuilders.standaloneSetup(new PlatformAssignmentAPI(service, org.mockito.Mockito.mock(com.ingot.cloud.iam.assignment.PlatformAuthorizationEditor.class))).build()
                .perform(get("/v1/platform/assignments").param("effectiveStatus", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
