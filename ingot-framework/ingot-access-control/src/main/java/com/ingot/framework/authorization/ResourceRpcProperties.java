package com.ingot.framework.authorization;

import java.util.List;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>独立资源RPC的服务器白名单；配置不从HTTP请求填充。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.iam.resource-rpc")
public class ResourceRpcProperties {

    /**
     * 本资源服务验签密钥；未设置时不发布内部对象端点。
     */
    private String secret;

    /**
     * IAM目录侧远程资源白名单；资源与服务一一显式配置。
     */
    private List<RemoteRegistration> registrations = List.of();

    /**
     * <p>服务器完整能力与固定服务名。
     * </p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Getter
    @Setter
    public static class RemoteRegistration {

        /**
         * 完整资源与可信操作读写模式。
         */
        private ResourceDescriptor descriptor;

        /**
         * 服务发现的稳定名称，不接受URL。
         */
        private String serviceName;

        /**
         * 资源服务专用密钥，通过环境变量或密钥系统注入。
         */
        private String secret;

        /** 纯字段清单 RPC 的专用服务签名密钥，不能使用登录成员令牌替代。 */
        private String manifestSecret;

    }

}
