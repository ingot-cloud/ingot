package com.ingot.framework.core.utils.sensitive;

/**
 * <p>Description  : SensitiveMode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/1.</p>
 * <p>Time         : 5:31 PM.</p>
 */
public enum SensitiveMode {

    /**
     * 自定义
     */
    CUSTOMER,
    /**
     * 用户名, 显示最后一个汉字
     */
    CHINESE_NAME,
    /**
     * 身份证号, 前六后四
     */
    ID_CARD,
    /**
     * 座机号, ****1234
     */
    FIXED_PHONE,
    /**
     * 手机号, 188****1234
     */
    MOBILE_PHONE,
    /**
     * 地址, 北京***
     */
    ADDRESS,
    /**
     * 电子邮件, m*****e@xxx.com
     */
    EMAIL,
    /**
     * 银行卡, 前六后四
     */
    BANK_CARD,
    /**
     * 密码, 永远是 ******
     */
    PASSWORD,
    /**
     * 密钥, 密钥除了最后三位其他都是***
     */
    KEY

}
