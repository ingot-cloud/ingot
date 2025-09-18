package com.ingot.framework.vc.module.email;

import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.common.InnerCheck;
import com.ingot.framework.vc.common.Utils;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCErrorCode;
import com.ingot.framework.vc.properties.EmailCodeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : DefaultEmailVCSendChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 4:47 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultEmailVCPreChecker implements VCPreChecker {
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailCodeProperties properties;

    @Override
    public void beforeSend(String receiver, String remoteIP) {
        InnerCheck.check(StrUtil.isNotEmpty(receiver),
                VCErrorCode.Check, "vc.check.email.receiverNotNull");

        // 当前邮件当前ip短信发送频率
        String rateKey = VCConstants.getEmailCheckKey(receiver, remoteIP);
        Integer rateCount = (Integer) redisTemplate.opsForValue().get(rateKey);
        if (rateCount != null) {
            log.error("[验证码] - 邮件前置检查器 - 操作频率过快 (发送) receiver={}, remoteIP={}", receiver, remoteIP);
            Utils.throwCheckException("vc.check.email.rate");
        } else {
            redisTemplate.opsForValue().set(rateKey, 1, properties.getOpsRate(), TimeUnit.SECONDS);
        }

    }

    @Override
    public void beforeCheck(String remoteIP) {
        int limitCount = properties.getOpsLimitCheckPerMinute();
        if (limitCount <= 0) {
            return;
        }

        String key = VCConstants.getEmailCheckKey("check", remoteIP);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count != null) {
            if (count > limitCount) {
                log.error("[验证码] - 邮件前置检查器 - 操作频率过快 (校验)  remoteIP={}", remoteIP);
                Utils.throwCheckException("vc.check.email.rate");
            }
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (expire != null && expire > 0) {
                redisTemplate.opsForValue().set(key, ++count, expire, TimeUnit.SECONDS);
            }
        } else {
            redisTemplate.opsForValue().set(key, 1, 60, TimeUnit.SECONDS);
        }
    }
}
