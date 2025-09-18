package com.ingot.framework.vc.module.sms;

import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.common.InnerCheck;
import com.ingot.framework.vc.common.Utils;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCErrorCode;
import com.ingot.framework.vc.properties.SMSCodeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : 默认短信发送校验.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/28.</p>
 * <p>Time         : 1:38 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultSmsVCPreChecker implements VCPreChecker {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SMSCodeProperties properties;

    @Override
    public void beforeSend(String receiver, String remoteIP) {
        InnerCheck.check(StrUtil.isNotEmpty(receiver),
                VCErrorCode.Check, "vc.check.sms.receiverNotNull");

        // 当前手机号当前ip短信发送频率
        String rateKey = VCConstants.getSmsCheckKey(receiver, remoteIP, "rate");
        Integer rateCount = (Integer) redisTemplate.opsForValue().get(rateKey);
        if (rateCount != null) {
            log.error("[验证码] - 短信发送检查器 - 操作频率过快 receiver={}, remoteIP={}", receiver, remoteIP);
            Utils.throwCheckException("vc.check.sms.rate");
        } else {
            redisTemplate.opsForValue().set(rateKey, 1, properties.getOpsRate(), TimeUnit.SECONDS);
        }

        // 今日当前手机号当前ip短信发送总数
        String ipPhoneSendCountOfDayKey = VCConstants.getSmsCheckKey(receiver, remoteIP, "ipPhoneCount");
        int ipPhoneLimitCountOfDay = properties.getIpPhoneLimitCountOfDay();
        checkDayCount(ipPhoneSendCountOfDayKey, ipPhoneLimitCountOfDay,
                "vc.check.sms.ipPhone", "手机号+IP短信发送数超限",
                receiver, remoteIP);

        // 今日receiver和ip都传手机号，用于检查手机号当天发送短信总数
        String phoneSendCountOfDayKey = VCConstants.getSmsCheckKey(receiver, receiver, "phoneCount");
        int phoneLimitCountOfDay = properties.getPhoneLimitCountOfDay();
        checkDayCount(phoneSendCountOfDayKey, phoneLimitCountOfDay,
                "vc.check.sms.phone", "手机号短信发送数超限",
                receiver, remoteIP);

        // 今日receiver和ip都传ip，用于检查当前ip发送短信总数
        String ipSendCountOfDayKey = VCConstants.getSmsCheckKey(remoteIP, remoteIP, "ipCount");
        int ipLimitCountOfDay = properties.getIpLimitCountOfDay();
        checkDayCount(ipSendCountOfDayKey, ipLimitCountOfDay,
                "vc.check.sms.ip", "IP短信发送数超限",
                receiver, remoteIP);

        // 今日短信发送总数
        String countOfDayKey = VCConstants.getSmsCheckKey("total", "total", "count");
        int limitCountOfDay = properties.getLimitCountOfDay();
        checkDayCount(countOfDayKey, limitCountOfDay,
                "vc.check.sms.total", "短信发送数超限",
                receiver, remoteIP);

    }

    @Override
    public void beforeCheck(String remoteIP) {
        int limitCount = properties.getOpsLimitCheckPerMinute();
        if (limitCount <= 0) {
            return;
        }

        String key = VCConstants.getSmsCheckKey("check", remoteIP, "check");
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count != null) {
            if (count > limitCount) {
                log.error("[验证码] - 短信前置检查器 - 操作频率过快 (校验)  remoteIP={}", remoteIP);
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

    private void checkDayCount(String key, int limitCount,
                               String messageCode, String checkTypeMessage,
                               String receiver, String remoteIP) {
        if (limitCount <= 0) {
            return;
        }
        Integer countOfDay = (Integer) redisTemplate.opsForValue().get(key);
        if (countOfDay != null && countOfDay > limitCount) {
            log.error("[验证码] - 短信发送检查器 - " + checkTypeMessage +
                    " limit={} receiver={}, remoteIP={}", limitCount, receiver, remoteIP);
            Utils.throwCheckException(messageCode);
        } else {
            redisTemplate.opsForValue().set(key,
                    countOfDay == null ? 1 : countOfDay + 1,
                    1, TimeUnit.DAYS);
        }
    }


}
