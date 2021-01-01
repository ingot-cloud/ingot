package com.ingot.framework.feign.exception;

import lombok.Getter;

/**
 * <p>Description  : IngotFeignException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 5:29 下午.</p>
 */
public class IngotFeignException extends RuntimeException {
    @Getter
    private final String code;

    public IngotFeignException(String code, String message){
        super(message);
        this.code = code;
    }
}
