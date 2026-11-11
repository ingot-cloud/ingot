package com.ingot.cloud.iam.interfaces.http;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>验证 IAM 局部错误映射使用稳定码对应的 HTTP 状态，非 IAM 码保持 500。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamErrorHandlerTest {
    private final IamErrorHandler handler = new IamErrorHandler();

    @Test
    void iamReasonCodesUseDeclaredHttpStatus() {
        for (IamReasonCode reason : IamReasonCode.values()) {
            var response = handler.bizException(new BizException(reason), new org.springframework.mock.web.MockHttpServletRequest());
            assertEquals(reason.getHttpStatus(), response.getStatusCode().value());
            assertEquals(reason.getCode(), response.getBody().getCode());
            assertNull(response.getBody().getData());
        }
        assertEquals(HttpStatus.BAD_REQUEST.value(), IamReasonCode.INVALID_ARGUMENT.getHttpStatus());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), IamReasonCode.IDENTITY_INVALID.getHttpStatus());
        assertEquals(HttpStatus.FORBIDDEN.value(), IamReasonCode.ACTION_DENIED.getHttpStatus());
        assertEquals(HttpStatus.NOT_FOUND.value(), IamReasonCode.OBJECT_NOT_FOUND.getHttpStatus());
        assertEquals(HttpStatus.CONFLICT.value(), IamReasonCode.REVISION_CONFLICT.getHttpStatus());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(),
                IamReasonCode.AUTHORIZATION_UNAVAILABLE.getHttpStatus());
    }

    @Test
    void foreignBizCodesStayInternalServerError() {
        var response = handler.bizException(new BizException("OtherServiceError", "其他服务错误"), new org.springframework.mock.web.MockHttpServletRequest());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("OtherServiceError", response.getBody().getCode());
    }

    @Test
    void actualV2ControllerUsesContractStatusesAndEmptyErrorData() throws Exception {
        var client = org.mockito.Mockito.mock(com.ingot.framework.authorization.AuthorizationClient.class);
        var controller = new com.ingot.cloud.iam.interfaces.inner.InnerResourceAuthorizationAPI(client);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(handler).build();
        String body = """
            {"resource":{"domain":"PLATFORM","applicationCode":"iam-ops","resourceCode":"incident"},
             "actionCodes":["iam-ops:incident:read"]}
            """;
        for (var reason : java.util.List.of(IamReasonCode.INVALID_ARGUMENT, IamReasonCode.ACTION_DENIED,
                IamReasonCode.AUTHORIZATION_UNAVAILABLE)) {
            org.mockito.Mockito.doThrow(new BizException(reason)).when(client)
                .evaluate(org.mockito.ArgumentMatchers.any());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/inner/authorization/v2/evaluate").contentType("application/json").content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                    .is(reason.getHttpStatus()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code")
                    .value(reason.getCode()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data")
                    .doesNotExist());
        }
    }
}
