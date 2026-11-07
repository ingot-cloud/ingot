package com.ingot.cloud.iam.application.id;

import java.util.Random;

import com.ingot.framework.id.BizGenerator;
import com.ingot.framework.id.config.IdAutoConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.stereotype.Component;

/**
 * <p>使用现有业务序列生成应用标识，保留前缀和随机低位规则。</p>
 * @author wangchao
 * @since 1.0.0
 */
@Component
@AutoConfigureAfter(IdAutoConfig.class)
@RequiredArgsConstructor
public class ApplicationIdGenerator {
    private static final String APPLICATION_SEQUENCE_KEY = "app_id";
    private static final String APPLICATION_ID_PREFIX = "in_";
    private static final int RANDOM_BIT_COUNT = 6;
    private static final int RANDOM_BOUND = 1 << RANDOM_BIT_COUNT;
    private static final Random RANDOM = new Random();
    private final BizGenerator bizGenerator;

    /**
     * 生成应用标识。
     * @return 与原发号协议一致的应用标识
     */
    public String genAppIdCode() {
        long id = bizGenerator.getId(APPLICATION_SEQUENCE_KEY);
        return APPLICATION_ID_PREFIX + (id << RANDOM_BIT_COUNT | RANDOM.nextInt(RANDOM_BOUND));
    }
}
