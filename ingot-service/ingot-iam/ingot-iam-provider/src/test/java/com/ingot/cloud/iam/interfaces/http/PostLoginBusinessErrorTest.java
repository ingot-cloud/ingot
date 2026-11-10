package com.ingot.cloud.iam.interfaces.http;

import com.ingot.cloud.iam.application.account.PlatformAccountService;
import com.ingot.cloud.iam.application.assignment.workspace.PlatformRoleWorkspace;
import com.ingot.cloud.iam.application.catalog.CatalogFacade;
import com.ingot.cloud.iam.application.delegation.TenantDelegationFacade;
import com.ingot.cloud.iam.application.group.GroupService;
import com.ingot.cloud.iam.application.organization.DepartmentEditService;
import com.ingot.cloud.iam.application.organization.DepartmentLeaderService;
import com.ingot.cloud.iam.application.organization.DepartmentNavigationService;
import com.ingot.cloud.iam.application.organization.DepartmentService;
import com.ingot.cloud.iam.application.organization.MemberFacade;
import com.ingot.cloud.iam.application.organization.OrganizationMemberQueryService;
import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformAccountAPI;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformApplicationAPI;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformGroupAPI;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformRoleCommandAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantDelegationAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantDepartmentAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantGroupAPI;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.core.error.GlobalExceptionHandlerResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>校验登录后管理入口保留错误码并返回业务文案。</p>
 * @author jy
 * @since 1.0.0
 */
class PostLoginBusinessErrorTest {
    private final GroupService groups = mock(GroupService.class);
    private final PlatformAccountService accounts = mock(PlatformAccountService.class);
    private final PlatformRoleFacade roles = mock(PlatformRoleFacade.class);
    private final TenantDelegationFacade delegations = mock(TenantDelegationFacade.class);
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new PlatformAccountAPI(accounts),
            new PlatformApplicationAPI(mock(CatalogFacade.class)),
            new PlatformRoleCommandAPI(roles, mock(PlatformRoleWorkspace.class)),
            new TenantDelegationAPI(delegations),
            new TenantDepartmentAPI(mock(DepartmentService.class), mock(DepartmentLeaderService.class),
                    mock(DepartmentEditService.class), mock(DepartmentNavigationService.class)),
            new PlatformGroupAPI(groups, mock(MemberFacade.class)),
            new TenantGroupAPI(groups, mock(OrganizationMemberQueryService.class)))
            .setControllerAdvice(new IamErrorHandler(), new GroupInputErrorHandler(), new GlobalExceptionHandlerResolver())
            .build();

    @Test
    void missingAndInvalidQueryParametersHaveSpecificMessages() throws Exception {
        mvc.perform(get("/v1/platform/applications")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("InvalidArgument"))
                .andExpect(jsonPath("$.message").value("请选择应用所属域"));
        mvc.perform(get("/v1/platform/applications").param("domain", "invalid")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("应用所属域无效"));
        mvc.perform(get("/v1/tenant/departments")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择部门查询用途"));
        mvc.perform(get("/v1/tenant/departments").param("purpose", "invalid")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("部门查询用途无效"));
    }

    @Test
    void groupBodiesAndHiddenDetailsUseTheirBusinessContext() throws Exception {
        for (var domain : AuthorizationDomain.values()) {
            String path = "/v1/" + domain.name().toLowerCase(java.util.Locale.ROOT) + "/groups";
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("InvalidArgument"))
                    .andExpect(jsonPath("$.message").value("用户组请求格式有误，请检查后重试"));
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":""}
                            """))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("S0003"))
                    .andExpect(jsonPath("$.message").value(containsString("请输入用户组名称")));
            when(groups.get(domain, "123")).thenThrow(new BizException(IamReasonCode.OBJECT_NOT_FOUND));
            mvc.perform(get(path + "/123")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ObjectNotFound"))
                    .andExpect(jsonPath("$.message").value("用户组不存在或不可访问"));
        }
    }

    @Test
    void missingAccountRoleRevisionAndDelegationNameTheObjectInTheMessage() throws Exception {
        when(accounts.get("123")).thenThrow(new BizException(IamReasonCode.OBJECT_NOT_FOUND));
        when(roles.getRevision(AuthorizationDomain.PLATFORM, false, "123", "456"))
                .thenThrow(new BizException(IamReasonCode.OBJECT_NOT_FOUND));
        when(delegations.get("123")).thenThrow(new BizException(IamReasonCode.OBJECT_NOT_FOUND));

        expectNotFound("/v1/platform/accounts/123", "账号不存在或不可访问");
        expectNotFound("/v1/platform/roles/123/revisions/456", "角色版本不存在或不可访问");
        expectNotFound("/v1/tenant/delegations/123", "委派记录不存在或不可访问");
    }

    private void expectNotFound(String path, String message) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(IamReasonCode.OBJECT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(message));
    }
}
