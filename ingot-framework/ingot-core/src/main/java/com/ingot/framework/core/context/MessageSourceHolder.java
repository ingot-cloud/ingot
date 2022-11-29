package com.ingot.framework.core.context;

import com.ingot.framework.core.config.MessageSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;

/**
 * <p>Description  : 用于加载{@link MessageSourceConfig#BASENAME}中的message.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/29.</p>
 * <p>Time         : 1:44 PM.</p>
 */
@Slf4j
public final class MessageSourceHolder {

    /**
     * 获取Message
     *
     * @param code 编码
     * @return message, 如果不存在那么抛出 {@link NoSuchMessageException}
     */
    public static String getMessage(String code) {
        return getMessageSource().getMessage(code);
    }

    /**
     * 获取Message
     *
     * @param code           编码
     * @param defaultMessage 默认message
     * @return message
     */
    public static String getMessage(String code, String defaultMessage) {
        return getMessageSource().getMessage(code, defaultMessage);
    }

    /**
     * 获取Message
     *
     * @param code 编码
     * @param args 参数
     * @return message
     */
    public static String getMessage(String code, Object... args) {
        return getMessageSource().getMessage(code, args);
    }

    private static IngotMessageSource getMessageSource() {
        return SpringContextHolder.getBean(IngotMessageSource.class);
    }
}
