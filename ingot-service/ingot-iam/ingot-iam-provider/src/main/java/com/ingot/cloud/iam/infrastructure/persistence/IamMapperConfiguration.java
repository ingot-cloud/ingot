package com.ingot.cloud.iam.infrastructure.persistence;

import com.ingot.cloud.iam.infrastructure.persistence.mapper.IdAllocationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamIdentityMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * <p>显式扫描IAM业务与辅助功能Mapper包，避免其他框架 MapperScan 使默认扫描退让。</p>
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = {IdAllocationMapper.class, IamIdentityMapper.class})
public class IamMapperConfiguration {
}
