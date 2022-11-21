package com.ingot.cloud.auth.web;

import javax.servlet.http.HttpServletRequest;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.core.context.ClientContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
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
    private final RegisteredClientRepository registeredClientRepository;

    @Permit
    @GetMapping("/login")
    public ModelAndView loginPage(ModelAndView modelAndView,
                                  HttpServletRequest request,
                                  @RequestParam(required = false) String error) {
        log.info("login clientId={}", ClientContextHolder.get());
        log.info("login error={}", error);
//        RegisteredClient client = registeredClientRepository.findByClientId();

        modelAndView.setViewName("login/index");
        modelAndView.addObject("error", error);
        modelAndView.addObject("testMsg", "hahah");
        request.setAttribute("test2", "user3");
        return modelAndView;
    }
}
