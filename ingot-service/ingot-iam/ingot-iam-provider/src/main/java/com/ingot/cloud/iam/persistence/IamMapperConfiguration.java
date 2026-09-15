package com.ingot.cloud.iam.persistence;

import com.ingot.cloud.iam.mapper.SysUserMapper;
import com.ingot.cloud.iam.persistence.mapper.IamIdentityMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * <p>显式扫描 IAM 新旧 Mapper 包，避免其他框架 MapperScan 使默认扫描退让。</p>
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = {SysUserMapper.class, IamIdentityMapper.class})
public class IamMapperConfiguration {
}
