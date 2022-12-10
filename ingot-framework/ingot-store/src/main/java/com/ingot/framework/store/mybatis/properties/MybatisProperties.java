package com.ingot.framework.store.mybatis.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : MybatisProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/10.</p>
 * <p>Time         : 1:33 PM.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.mybatis")
public class MybatisProperties {
    private boolean showSqlLog;
}
