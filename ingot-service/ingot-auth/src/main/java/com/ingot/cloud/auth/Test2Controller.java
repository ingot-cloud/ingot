package com.ingot.cloud.auth;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : Test2Controller.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/22.</p>
 * <p>Time         : 5:45 下午.</p>
 */
@Permit
@RequestMapping("/testtest")
@RestController
public class Test2Controller {

    @GetMapping
    public String a() {
        return "a";
    }

    @PostMapping
    public String b() {
        return "b";
    }

    @GetMapping("/c")
    public String c() {
        return "c";
    }
}
