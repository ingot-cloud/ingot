package com.ingot.framework.vc.properties;

import java.util.List;

import lombok.Data;

/**
 * <p>Description  : SMSCodeProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/23.</p>
 * <p>Time         : 7:41 PM.</p>
 */
@Data
public class SMSCodeProperties {
    /**
     * 要拦截的url, ant pattern
     */
    private List<String> urls;
    /**
     * 验证码长度
     */
    private int length = 6;
    /**
     * 过期时间，单位秒
     */
    private int expireIn = 60;
    /**
     * 每天每个手机号最大送送短信数量
     */
    private int mobileMaxSendCount;
    /**
     * 每天每个IP最大送送短信数量
     */
    private int ipMaxSendCount;
    /**
     * 每天最大发送短信数量
     */
    private int totalMaxSendCount;
}
