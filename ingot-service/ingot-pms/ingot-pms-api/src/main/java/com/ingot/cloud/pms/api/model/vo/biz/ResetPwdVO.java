package com.ingot.cloud.pms.api.model.vo.biz;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : ResetPwdVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/21.</p>
 * <p>Time         : 10:44 AM.</p>
 */
@Data
public class ResetPwdVO implements Serializable {
    /**
     * 初始化随机验证码
     */
    private String random;
}
