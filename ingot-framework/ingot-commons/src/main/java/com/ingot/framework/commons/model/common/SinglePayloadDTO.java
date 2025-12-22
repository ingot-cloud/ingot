package com.ingot.framework.commons.model.common;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : SingleDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/22.</p>
 * <p>Time         : 16:01.</p>
 */
@Data
public class SinglePayloadDTO<T> implements Serializable {
    /**
     * 载体
     */
    private T payload;
}
