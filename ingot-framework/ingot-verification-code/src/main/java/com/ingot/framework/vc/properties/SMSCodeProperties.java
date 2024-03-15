package com.ingot.framework.vc.properties;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>Description  : SMSCodeProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/23.</p>
 * <p>Time         : 7:41 PM.</p>
 */
@Getter
@Setter
public class SMSCodeProperties {
    /**
     * 验证码长度
     */
    private int length = 6;
    /**
     * 过期时间，单位秒
     */
    private int expireIn = 5 * 60;
    /**
     * 操作频率，单位秒
     */
    private int opsRate = 60;
    /**
     * 每天每个手机号最大短信发送数量, 0表示无限制
     */
    private int phoneLimitCountOfDay = 0;
    /**
     * 每天每个IP最大短信发送数量, 0表示无限制
     */
    private int ipLimitCountOfDay = 0;
    /**
     * 每天每个IP每个手机号最大短信发送数量，0表示无限制
     */
    private int ipPhoneLimitCountOfDay = 0;
    /**
     * 每天最大短信发送数量, 0表示无限制
     */
    private int limitCountOfDay = 0;
    /**
     * 每分钟检查次数限制，0表示无限制
     */
    private int opsLimitCheckPerMinute = 0;
}
