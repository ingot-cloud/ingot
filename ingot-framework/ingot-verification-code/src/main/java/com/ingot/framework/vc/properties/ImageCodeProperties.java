package com.ingot.framework.vc.properties;

import lombok.Data;

import java.util.Map;

/**
 * <p>Description  : ImageCodeProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/23.</p>
 * <p>Time         : 7:41 PM.</p>
 */
@Data
public class ImageCodeProperties {
    /**
     * 默认验证码模式
     */
    private String model;
    /**
     * 指定url特定验证码模式
     */
    private Map<String, String> urlModel;
    /**
     * ip 模式，触发验证持续时间，单位秒
     */
    private int ipModelDurationTime = 3600;
    /**
     * ip 模式间隔时间，单位秒
     */
    private int ipModelIntervalTime = 5;
    /**
     * ip 模式触发验证次数阈值
     */
    private int ipModelThreshold = 3;
    /**
     * 验证码长度
     */
    private int length = 4;
    /**
     * 过期时间
     */
    private int expireIn = 60;
}
