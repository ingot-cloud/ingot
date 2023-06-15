package com.ingot.framework.vc.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

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
     * 到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime expireTime;

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
        this.expireTime = LocalDateTime.now().plusSeconds(expireIn);
    }

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
        return "VC{" + "value='" + value + '\'' +
                ", type='" + type + '\'' +
                ", expireTime=" + expireTime +
                '}';
    }
}
