package com.ingot.framework.vc.module.captcha;

import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.common.Utils;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.properties.ImageCodeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * <p>Description  : DefaultCaptchaVCSendChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/25.</p>
 * <p>Time         : 3:07 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCaptchaVCPreChecker implements VCPreChecker {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ImageCodeProperties properties;

    @Override
    public void beforeSend(String receiver, String remoteIP) {
        int limitCount = properties.getOpsLimitGetPerMinute();
        if (limitCount <= 0) {
            return;
        }

        String key = VCConstants.getCaptchaCheckKey("get", remoteIP);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count != null) {
            if (count > limitCount) {
                log.error("[验证码] - 图片验证码前置检查器 - 操作频率过快 (获取)  remoteIP={}", remoteIP);
                Utils.throwCheckException("vc.check.sms.rate");
            }
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (expire != null && expire > 0) {
                redisTemplate.opsForValue().set(key, ++count, expire, TimeUnit.SECONDS);
            }
        } else {
            redisTemplate.opsForValue().set(key, 1, 60, TimeUnit.SECONDS);
        }
    }

    @Override
    public void beforeCheck(String remoteIP) {
        int limitCount = properties.getOpsLimitCheckPerMinute();
        if (limitCount <= 0) {
            return;
        }

        String key = VCConstants.getCaptchaCheckKey("check", remoteIP);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count != null) {
            if (count > limitCount) {
                log.error("[验证码] - 图片验证码前置检查器 - 操作频率过快 (校验)  remoteIP={}", remoteIP);
                Utils.throwCheckException("vc.check.sms.rate");
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
