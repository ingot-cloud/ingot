package com.ingot.cloud.auth;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : TestInnerController.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/23.</p>
 * <p>Time         : 3:09 下午.</p>
 */
@RestController
public class TestInnerController {

    @Permit(mode = PermitMode.INNER)
    @GetMapping("/inner")
    public String test() {
        return "ok";
    }
}
