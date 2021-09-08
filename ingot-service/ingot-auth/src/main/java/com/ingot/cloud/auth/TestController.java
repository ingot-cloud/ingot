package com.ingot.cloud.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : TestController.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/7.</p>
 * <p>Time         : 5:26 下午.</p>
 */
@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "haha";
    }
}
