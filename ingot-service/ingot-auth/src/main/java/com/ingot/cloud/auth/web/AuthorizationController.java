package com.ingot.cloud.auth.web;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Permit
    @GetMapping("/login")
    public ModelAndView loginPage(ModelAndView modelAndView,
                                  @RequestParam(required = false) String error) {
        modelAndView.setViewName("login/index");
        return modelAndView;
    }
}
