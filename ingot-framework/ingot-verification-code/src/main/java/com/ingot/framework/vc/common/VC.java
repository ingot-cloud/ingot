package com.ingot.framework.vc.common;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * <p>Description  : VerificationCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/28.</p>
 * <p>Time         : 11:42 PM.</p>
 */
@Data
public class VC implements Serializable {
    /**
     * 验证码类型
     */
    private VCType type;
    /**
     * 验证码
     */
    private String code;
    /**
     * 过期时间
     */
    private int expireIn;
    /**
     * 到期时间
     */
    private LocalDateTime expireTime;

    /**
     * Is expired boolean.
     *
     * @return the boolean
     */
    @JsonIgnore
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * To string string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "VC{" + "code='" + code + '\'' +
                ", type='" + type + '\'' +
                ", expireTime=" + expireTime +
                '}';
    }
}
