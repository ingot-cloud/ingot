package com.ingot.framework.security.recording.store.mysql.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.ingot.framework.security.recording.store.mysql.mapper.MapperModule;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/**
 * <p>独立注册 {@code SecurityEventStoreMapper}，与 Store bean 分文件以便自动配置阶段可见。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConditionalOnClass({DataSource.class, SqlSessionFactory.class})
@MapperScan(basePackageClasses = MapperModule.class)
public class SecurityEventStoreMysqlMapperAutoConfiguration {
}
