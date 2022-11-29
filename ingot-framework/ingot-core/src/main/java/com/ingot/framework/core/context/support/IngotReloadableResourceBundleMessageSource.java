package com.ingot.framework.core.context.support;

import com.ingot.framework.core.context.IngotMessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * <p>Description  : IngotReloadableResourceBundleMessageSource.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/29.</p>
 * <p>Time         : 2:26 PM.</p>
 */
public class IngotReloadableResourceBundleMessageSource extends ReloadableResourceBundleMessageSource
        implements IngotMessageSource {
}
