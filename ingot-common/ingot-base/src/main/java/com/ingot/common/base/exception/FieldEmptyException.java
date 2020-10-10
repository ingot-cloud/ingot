package com.ingot.common.base.exception;

import static com.ingot.common.base.http.BaseStatusCode.FIELD_EMPTY;

/**
 * <p>Description  : FieldEmptyException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/10.</p>
 * <p>Time         : 4:14 下午.</p>
 */
public class FieldEmptyException extends BaseException {
    public FieldEmptyException(String field) {
        super(FIELD_EMPTY, field);
    }
}
