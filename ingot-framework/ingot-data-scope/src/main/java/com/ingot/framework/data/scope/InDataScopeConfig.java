package com.ingot.framework.data.scope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import javax.sql.DataSource;

/**
 * <p>Description  : InDataScopeConfig.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/14.</p>
 * <p>Time         : 15:46.</p>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnBean(DataSource.class)
public class InDataScopeConfig {


}
