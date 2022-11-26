package com.ingot.cloud.auth.web;

import javax.servlet.http.HttpSession;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * <p>Description  : AuthorizationController.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/19.</p>
 * <p>Time         : 1:55 PM.</p>
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class AuthorizationController {
    private final RegisteredClientRepository registeredClientRepository;

    @Permit
    @GetMapping("/login")
    public ModelAndView loginPage(ModelAndView modelAndView,
                                  HttpSession session) {
        // 从session中获取错误信息
        Exception exception = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        String error = exception != null ? exception.getLocalizedMessage() : "";

        modelAndView.setViewName("login/index");
        modelAndView.addObject("error", error);
        return modelAndView;
    }
}
