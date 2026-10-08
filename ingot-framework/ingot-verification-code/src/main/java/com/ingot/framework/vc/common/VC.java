package com.ingot.framework.vc.common;

import java.io.Serializable;
import java.time.Instant;

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
    private String value;
    /**
     * 过期时间，单位秒
     */
    private int expireIn;
    /**
     * 到期时间点（UTC），不随 JVM 时区变化
     */
    private Instant expireTime;

    /**
     * 实例化
     *
     * @param type     {@link VCType}
     * @param value    验证码
     * @param expireIn 过期时间
     * @return {@link VC}
     */
    public static VC instance(VCType type, String value, int expireIn) {
        return new VC(type, value, expireIn);
    }

    public VC() {
    }

    public VC(VCType type, String value, int expireIn) {
        this.type = type;
        this.value = value;
        this.expireIn = expireIn;
        this.expireTime = Instant.now().plusSeconds(expireIn);
    }

    /**
     * Is expired boolean.
     *
     * @return the boolean
     */
    @JsonIgnore
    public boolean isExpired() {
        return !Instant.now().isBefore(expireTime);
    }

    /**
     * To string string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "VC{" + "value='" + value + '\'' +
                ", type='" + type + '\'' +
                ", expireTime=" + expireTime +
                '}';
    }
}
