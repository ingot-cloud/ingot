package com.ingot.framework.vc;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * <p>Description  : VerificationCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/28.</p>
 * <p>Time         : 11:42 PM.</p>
 */
@Data
public class VerificationCode implements Serializable {
    private String type;
    private String code;
    private int expireIn;
    private LocalDateTime expireTime;

}
