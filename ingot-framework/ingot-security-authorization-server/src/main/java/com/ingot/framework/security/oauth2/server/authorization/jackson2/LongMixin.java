package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * <p>Description  : LongMixin.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/11.</p>
 * <p>Time         : 2:39 下午.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
abstract class LongMixin {
    @JsonCreator
    static void valueOf(String s) {
    }
}
