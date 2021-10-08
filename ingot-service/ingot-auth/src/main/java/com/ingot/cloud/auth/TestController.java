package com.ingot.cloud.auth;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : TestController.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/7.</p>
 * <p>Time         : 5:26 下午.</p>
 */
@Slf4j
@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        log.info("----- user={}", SecurityAuthContext.getUser());
        log.info("----- roles={}", SecurityAuthContext.getRoles());
        return "haha:" + SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Permit
    @GetMapping("/test2")
    public String test2() {
        return "permit url";
    }
}
