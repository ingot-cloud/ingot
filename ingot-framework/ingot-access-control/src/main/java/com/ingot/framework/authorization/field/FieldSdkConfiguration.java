package com.ingot.framework.authorization.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.authorization.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>公共字段绑定、投影及事务门禁的自动装配，业务服务可以替换策略提供者。</p>
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration(after = {AuthorizationSdkConfiguration.class, org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class})
@EnableConfigurationProperties(FieldManifestProperties.class)
public class FieldSdkConfiguration {
    /** 仅在配置专用服务密钥时开放纯元数据端点。 */
    @Bean @ConditionalOnProperty(prefix = "ingot.iam.field-manifest", name = "secret")
    public FieldManifestEndpoint fieldManifestEndpoint(FieldBindingRegistry fields, ResourceRegistry resources,
            ObjectMapper mapper, FieldManifestProperties properties) {
        com.ingot.framework.authorization.ResourceRpcSigner.sign("", properties.getSecret());
        return new FieldManifestEndpoint(fields, resources, mapper, properties);
    }
    /** 本服务的启动编译注册表。 */
    @Bean @ConditionalOnMissingBean
    public FieldBindingRegistry fieldBindingRegistry(ObjectMapper mapper) { return new FieldBindingRegistry(mapper); }

    /** 无脚本的默认脱敏执行器。 */
    @Bean @ConditionalOnMissingBean(MaskStrategy.class)
    public DefaultMaskStrategy fieldMaskStrategy() { return new DefaultMaskStrategy(); }

    /** 不修改原对象的 JSON 投影器。 */
    @Bean @ConditionalOnMissingBean
    public FieldProjectionEngine fieldProjectionEngine(ObjectMapper mapper, FieldBindingRegistry registry, MaskStrategy masks) {
        return new FieldProjectionEngine(mapper, registry, masks);
    }

    /** 显式资源动作的策略提供者。 */
    @Bean @ConditionalOnMissingBean(FieldPolicyProvider.class) @ConditionalOnBean(AuthorizationAccess.class)
    public ClientFieldPolicyProvider fieldPolicyProvider(AuthorizationAccess access, ResourceRegistry registry) {
        return new ClientFieldPolicyProvider(access, registry);
    }

    /** 事务内最终字段写门禁。 */
    @Bean @ConditionalOnMissingBean @ConditionalOnBean(FieldPolicyProvider.class)
    public FieldWriteExecutor fieldWriteExecutor(FieldPolicyProvider policies) { return new FieldWriteExecutor(policies); }

    /** 普通及嵌套单对象 DTO 自动校验原始 JSON 属性；批量命令逐对象执行最终门禁。 */
    @Bean @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FieldInputAdvice fieldInputAdvice(ObjectMapper mapper, FieldBindingRegistry fields) {
        return new FieldInputAdvice(mapper, fields);
    }

    /** 原始 GET 条件在绑定阶段检查，查询范围证明由业务上下文提供。 */
    @Bean @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public org.springframework.web.servlet.config.annotation.WebMvcConfigurer fieldFilterConfigurer(ObjectMapper mapper, FieldBindingRegistry fields) {
        var interceptor = new FieldFilterInterceptor(mapper, fields);
        return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
            @Override public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }

    /** 编译实际 HTTP 注解接入点。 */
    @Bean @ConditionalOnMissingBean @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnBean(RequestMappingHandlerMapping.class)
    public FieldBindingRegistrar fieldBindingRegistrar(RequestMappingHandlerMapping handlers, FieldBindingRegistry registry) {
        return new FieldBindingRegistrar(handlers, registry);
    }
}
