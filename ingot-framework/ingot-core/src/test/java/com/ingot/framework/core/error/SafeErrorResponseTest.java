package com.ingot.framework.core.error;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * <p>验证客户端提示只包含可公开信息，业务和访问拒绝仍按原协议。</p>
 * @author jy
 * @since 1.0.0
 */
class SafeErrorResponseTest {
    private static final String DIAGNOSTIC = "fixture-private-diagnostic";
    private final org.springframework.test.web.servlet.MockMvc mvc = standaloneSetup(new FixtureController())
            .setControllerAdvice(new GlobalExceptionHandlerResolver()).build();

    @Test
    void mvcHidesUnexpectedAndInternalBusinessDiagnostics() throws Exception {
        for (String path : new String[]{"/unexpected", "/internal"}) {
            mvc.perform(get(path)).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(BaseErrorCode.INTERNAL_SERVER_ERROR.getCode()))
                    .andExpect(jsonPath("$.message").value(BaseErrorCode.INTERNAL_SERVER_ERROR.getText()))
                    .andExpect(content().string(not(containsString(DIAGNOSTIC))));
        }
        mvc.perform(get("/denied")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(BaseErrorCode.FORBIDDEN.getCode()))
                .andExpect(content().string(not(containsString(DIAGNOSTIC))));
    }

    @Test
    void malformedBodyIsSafeAndBusinessAdviceStillReturnsItsMessage() throws Exception {
        mvc.perform(post("/input").contentType("application/json").content("{" + DIAGNOSTIC))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BaseErrorCode.ILLEGAL_REQUEST_PARAMS.getCode()))
                .andExpect(content().string(not(containsString(DIAGNOSTIC))));
        mvc.perform(get("/business")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("DomainExample"))
                .andExpect(jsonPath("$.message").value("请选择有效的角色"));
    }

    @Test
    void servletErrorDispatchNeverIncludesDiagnosticAttributes() {
        var request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException(DIAGNOSTIC));
        var body = new InErrorAttributes().getErrorAttributes(new ServletWebRequest(request),
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE));
        assertEquals(BaseErrorCode.INTERNAL_SERVER_ERROR.getCode(), body.get(R.CODE));
        assertEquals(BaseErrorCode.INTERNAL_SERVER_ERROR.getText(), body.get(R.MESSAGE));
        assertFalse(body.containsKey(R.DATA));
        assertFalse(body.toString().contains(DIAGNOSTIC));
    }

    /**
     * <p>制造已知业务失败和意外异常供框架响应测试。</p>
     * @author jy
     * @since 1.0.0
     */
    @RestController
    static class FixtureController {
        /** 模拟不可公开的意外异常。 */
        @GetMapping("/unexpected")
        public void unexpected() { throw new IllegalStateException(DIAGNOSTIC); }

        /** 模拟使用内部错误码的业务异常。 */
        @GetMapping("/internal")
        public void internal() { throw new BizException(BaseErrorCode.INTERNAL_SERVER_ERROR.getCode(), DIAGNOSTIC); }

        /** 模拟权限拒绝。 */
        @GetMapping("/denied")
        public void denied() { throw new AuthorizationDeniedException(DIAGNOSTIC, new AuthorizationDecision(false)); }

        /** 模拟可公开的业务原因。 */
        @GetMapping("/business")
        public void business() { throw new BizException("DomainExample", "请选择有效的角色"); }

        /** 接收 JSON 供反序列化失败测试。 */
        @PostMapping("/input")
        public Object input(@RequestBody java.util.Map<String, String> body) { return body; }
    }
}
