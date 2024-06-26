package com.ingot.framework.core.model.event;

import com.ingot.framework.core.model.common.AuthSuccessDTO;

/**
 * <p>Description  : LoginEvent.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/28.</p>
 * <p>Time         : 9:09 AM.</p>
 */
public record LoginEvent(AuthSuccessDTO payload) {
}
