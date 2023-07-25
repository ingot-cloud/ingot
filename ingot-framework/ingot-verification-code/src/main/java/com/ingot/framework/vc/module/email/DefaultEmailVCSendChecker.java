package com.ingot.framework.vc.module.email;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.common.InnerCheck;
import com.ingot.framework.vc.common.Utils;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCErrorCode;
import com.ingot.framework.vc.properties.EmailCodeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * <p>Description  : DefaultEmailVCSendChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 4:47 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultEmailVCSendChecker implements VCSendChecker {
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailCodeProperties properties;

    @Override
    public void check(String receiver, String remoteIP) {
        InnerCheck.check(StrUtil.isNotEmpty(receiver),
                VCErrorCode.Check, "vc.check.email.receiverNotNull");

        // 当前邮件当前ip短信发送频率
        String rateKey = VCConstants.getEmailCheckKey(receiver, remoteIP);
        Integer rateCount = (Integer) redisTemplate.opsForValue().get(rateKey);
        if (rateCount != null) {
            log.error("[验证码] - 邮件发送检查器 - 操作频率过快 receiver={}, remoteIP={}", receiver, remoteIP);
            Utils.throwCheckException("vc.check.email.rate");
        } else {
            redisTemplate.opsForValue().set(rateKey, 1, properties.getOpsRate(), TimeUnit.SECONDS);
        }

    }
}
