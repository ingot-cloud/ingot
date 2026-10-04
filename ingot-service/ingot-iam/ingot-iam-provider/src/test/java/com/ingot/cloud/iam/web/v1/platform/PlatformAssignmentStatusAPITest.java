package com.ingot.cloud.iam.web.v1.platform;

import java.util.List;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus;
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

    @ParameterizedTest
    @EnumSource(AssignmentEffectiveStatus.class)
    void bindsEachComputedStateAlongsideExistingFilters(AssignmentEffectiveStatus state) throws Exception {
        AssignmentService service = mock(AssignmentService.class);
        when(service.list(AuthorizationDomain.PLATFORM, 2, 20, SubjectType.GROUP, "运维", state))
                .thenReturn(new PageResponse<>(List.of(), 0, 2, 20));
        MockMvcBuilders.standaloneSetup(new PlatformAssignmentAPI(service)).build()
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
        MockMvcBuilders.standaloneSetup(new PlatformAssignmentAPI(service)).build()
                .perform(get("/v1/platform/assignments")).andExpect(status().isOk());
        verify(service).list(AuthorizationDomain.PLATFORM, 1, 20, null, null, null);
    }

    @Test
    void unknownStateIsRejectedBeforeTheListService() throws Exception {
        AssignmentService service = mock(AssignmentService.class);
        MockMvcBuilders.standaloneSetup(new PlatformAssignmentAPI(service)).build()
                .perform(get("/v1/platform/assignments").param("effectiveStatus", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
