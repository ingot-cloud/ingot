package com.ingot.cloud.iam.web;

import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>描述这个类的作用</p>
 *
 * @author jy
 * @since
 */
@RestController
@Tag(description = "流量测试接口", name = "")
@RequestMapping(value = "/test")
@RequiredArgsConstructor
public class TestAPI implements RShortcuts {

    @Permit
    @GetMapping("/limit")
    public R<?> test() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return ok("ok");
    }
}
