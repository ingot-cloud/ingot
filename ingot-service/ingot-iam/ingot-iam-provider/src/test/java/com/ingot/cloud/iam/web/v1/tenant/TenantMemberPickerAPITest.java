package com.ingot.cloud.iam.web.v1.tenant;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.organization.*;
import com.ingot.cloud.iam.web.IamErrorHandler;
import com.ingot.framework.authorization.SdkAuthorizationErrorHandler;
import com.ingot.framework.authorization.field.FieldBindingRegistry;
import com.ingot.framework.authorization.field.FieldFilterInterceptor;
import com.ingot.framework.authorization.field.FieldInputCollector;
import com.ingot.framework.commons.annotation.field.FieldControl;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.core.convert.StringToEnumConverterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <p>通过实际字段拦截器验证组织选人完整HTTP参数，避免仅服务层测试遗漏参数分类。</p>
 * @author jy
 * @since 1.0.0
 */
class TenantMemberPickerAPITest {
    private static final String BROWSE = "/v1/tenant/member-picker/browse";
    private static final String MEMBER_PICKER = "/v1/tenant/members/picker";
    private static final String LEADERS = "/v1/tenant/departments/1001300/leaders";
    private static final String CANDIDATES = "/v1/tenant/departments/1001300/leader-candidates";
    private final ObjectMapper mapper = new ObjectMapper();
    private final FieldBindingRegistry bindings = new FieldBindingRegistry(mapper);
    private final OrganizationMemberPickerService browse = mock(OrganizationMemberPickerService.class);
    private final OrganizationMemberService members = mock(OrganizationMemberService.class);
    private final DepartmentLeaderService leaders = mock(DepartmentLeaderService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var controllers = List.of(new TenantMemberPickerAPI(browse),
                new TenantMemberCommandAPI(mock(MemberCommandService.class), mock(MemberQueryService.class),
                        mock(MemberExportService.class), mock(TenantMemberOnboarding.class),
                        mock(com.ingot.cloud.iam.organization.MemberRejoin.class), members),
                new TenantDepartmentAPI(mock(DepartmentService.class), leaders,
                        mock(DepartmentEditService.class), mock(DepartmentNavigationService.class)));
        for (var controller : controllers) {
            for (var method : controller.getClass().getDeclaredMethods()) {
                for (var control : method.getAnnotationsByType(FieldControl.class)) {
                    if (control.use() == FieldUse.FILTER) {
                        bindings.register(control.valueType(), resource(control), control.action(), control.use());
                    }
                }
            }
        }
        var conversion = new DefaultFormattingConversionService();
        conversion.addConverterFactory(new StringToEnumConverterFactory());
        mvc = MockMvcBuilders.standaloneSetup(controllers.toArray())
                .setConversionService(conversion)
                .addInterceptors(new FieldFilterInterceptor(mapper, bindings))
                .setControllerAdvice(new IamErrorHandler(), new SdkAuthorizationErrorHandler()).build();
        when(browse.browse(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new OrganizationMemberPickerPage(true,
                        new DepartmentNavigationPage(List.of(), 0, 1, 20, List.of(), null),
                        new PageResponse<>(List.of(), 0, 1, 20)));
        var emptyLeaders = new DepartmentLeaderPage(List.of(), 0, 1, 20, "0", false, null);
        when(leaders.page(anyString(), any(), anyInt(), anyInt(), anyBoolean())).thenReturn(emptyLeaders);
        when(leaders.candidates(anyString(), any(), anyInt(), anyInt(), any())).thenReturn(emptyLeaders);
        when(members.page(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 1, 20));
    }

    @Test
    void reportedDepartmentLeaderRequestPassesActualFilterInterceptor() throws Exception {
        mvc.perform(get(BROWSE).param("scene", "DEPARTMENT_LEADER").param("targetDepartmentId", "1001300")
                        .param("page", "1").param("pageSize", "20")
                        .param("departmentPage", "1").param("departmentPageSize", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(browse).browse(OrganizationMemberPickerScene.DEPARTMENT_LEADER, null, null, "1001300", null,
                1, 20, 1, 20);
    }

    @Test
    void allScenesKeepNavigationSearchAndIndependentPages() throws Exception {
        for (var scene : OrganizationMemberPickerScene.values()) {
            var request = get(BROWSE).param("scene", scene.getValue()).param("departmentId", "1001103")
                    .param("keyword", "张").param("page", "2").param("pageSize", "10")
                    .param("departmentPage", "3").param("departmentPageSize", "5");
            String target = scene == OrganizationMemberPickerScene.DEPARTMENT_LEADER ? "1001300" : null;
            String source = scene == OrganizationMemberPickerScene.ASSIGNMENT ? "61" : null;
            if (target != null) request.param("targetDepartmentId", target);
            if (source != null) request.param("delegationGrantId", source);
            mvc.perform(request).andExpect(status().isOk());
            verify(browse).browse(scene, "1001103", "张", target, source, 2, 10, 3, 5);
        }
    }

    @Test
    void omittedPagingControlsStillUseExistingDefaults() throws Exception {
        mvc.perform(get(BROWSE).param("scene", "DEPARTMENT_LEADER").param("targetDepartmentId", "1001300"))
                .andExpect(status().isOk());
        verify(browse).browse(OrganizationMemberPickerScene.DEPARTMENT_LEADER, null, null, "1001300", null,
                1, 20, 1, 20);
    }

    @Test
    void existingMemberAndLeaderEndpointsAcceptAllTheirDeclaredControls() throws Exception {
        mvc.perform(get(MEMBER_PICKER).param("departmentId", "1001103").param("keyword", "张")
                        .param("ids", "101", "102").param("page", "2").param("pageSize", "10"))
                .andExpect(status().isOk());
        verify(members).page("1001103", "张", List.of("101", "102"), 2, 10);
        mvc.perform(get(LEADERS).param("keyword", "张").param("page", "2").param("pageSize", "10"))
                .andExpect(status().isOk());
        verify(leaders).page("1001300", "张", 2, 10, false);
        mvc.perform(get(CANDIDATES).param("keyword", "张").param("browseDepartmentId", "1001103")
                        .param("page", "2").param("pageSize", "10"))
                .andExpect(status().isOk());
        verify(leaders).candidates("1001300", "张", 2, 10, "1001103");
    }

    @Test
    void unknownAndOtherEndpointControlsAreRejectedBeforeQuerying() throws Exception {
        for (var path : List.of(BROWSE, MEMBER_PICKER, LEADERS, CANDIDATES)) {
            var request = get(path).param("phone", "unexpected");
            if (path.equals(BROWSE)) request.param("scene", "MEMBER");
            mvc.perform(request)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("InvalidArgument"));
        }
        mvc.perform(get(BROWSE).param("scene", "MEMBER").param("ids", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(MEMBER_PICKER).param("targetDepartmentId", "1001300"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(LEADERS).param("browseDepartmentId", "1001103"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(CANDIDATES).param("departmentPage", "2"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(browse, members, leaders);
    }

    @Test
    void businessAuthorizationDenialIsStillReturned() throws Exception {
        when(browse.browse(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        mvc.perform(get(BROWSE).param("scene", "DEPARTMENT_LEADER").param("targetDepartmentId", "1001300")
                        .param("page", "1").param("pageSize", "20"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ActionDenied"));
    }

    @Test
    void eachQueryMapsOnlyKeywordToControlledDisplayName() throws Exception {
        for (var type : List.of(TenantMemberPickerAPI.class, TenantMemberCommandAPI.class, TenantDepartmentAPI.class)) {
            for (var method : type.getDeclaredMethods()) {
                for (var control : method.getAnnotationsByType(FieldControl.class)) {
                    if (control.use() != FieldUse.FILTER || method.getName().equals("list")) continue;
                    var fields = FieldInputCollector.collect(mapper.readTree("{\"keyword\":\"张\"}"),
                            bindings.require(control.valueType(), resource(control), FieldUse.FILTER));
                    assertEquals(1, fields.size());
                    assertEquals(java.util.Set.of(MemberFieldKey.VALUE_DISPLAY_NAME),
                            fields.get(MemberResources.TENANT_MEMBER).keySet());
                }
            }
        }
    }

    private static ResourceKey resource(FieldControl control) {
        return new ResourceKey(control.domain(), control.applicationCode(), control.resourceCode());
    }
}
