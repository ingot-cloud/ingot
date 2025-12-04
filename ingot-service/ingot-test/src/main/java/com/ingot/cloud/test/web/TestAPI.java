package com.ingot.cloud.test.web;

import cn.hutool.core.util.RandomUtil;
import com.ingot.cloud.test.service.biz.KafkaService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : TestAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/4.</p>
 * <p>Time         : 13:18.</p>
 */
@RestController
@Tag(description = "", name = "")
@RequestMapping(value = "/test")
@RequiredArgsConstructor
public class TestAPI implements RShortcuts {

    private final KafkaService kafkaService;

    @Permit
    @PostMapping("/send")
    public R<?> send() {
        String message = RandomUtil.randomString(10);
        kafkaService.send(message);
        return ok(message);
    }
}
