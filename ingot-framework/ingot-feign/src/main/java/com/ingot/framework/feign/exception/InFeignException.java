package com.ingot.framework.feign.exception;

import lombok.Getter;

/**
 * <p>Description  : 自定义 Feign Exception.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 5:29 下午.</p>
 */
@Getter
public class InFeignException extends RuntimeException {
    private final String code;

    public InFeignException(String code, String message){
        super(message);
        this.code = code;
    }
}
