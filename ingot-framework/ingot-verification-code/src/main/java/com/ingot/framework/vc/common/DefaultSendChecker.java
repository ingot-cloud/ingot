package com.ingot.framework.vc.common;

import com.ingot.framework.vc.VCSendChecker;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : DefaultSendChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/29.</p>
 * <p>Time         : 11:06 AM.</p>
 */
@Slf4j
public class DefaultSendChecker implements VCSendChecker {
    public static final VCSendChecker DEFAULT = new DefaultSendChecker();

    @Override
    public void check(String receiver, String remoteIP) {
        log.info("[验证码] - 检测器缺省实现 - receiver={}, remoteIP={}", receiver, remoteIP);
    }
}
