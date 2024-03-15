package com.ingot.framework.vc.common;

import com.ingot.framework.vc.VCPreChecker;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : DefaultSendChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/29.</p>
 * <p>Time         : 11:06 AM.</p>
 */
@Slf4j
public class DefaultPreChecker implements VCPreChecker {
    public static final VCPreChecker DEFAULT = new DefaultPreChecker();

    @Override
    public void beforeSend(String receiver, String remoteIP) {
        log.info("[验证码] - 检测器 beforeSend 缺省实现 - receiver={}, remoteIP={}", receiver, remoteIP);
    }

    @Override
    public void beforeCheck(String remoteIP) {
        log.info("[验证码] - 检测器 beforeCheck 缺省实现 - remoteIP={}", remoteIP);
    }
}
