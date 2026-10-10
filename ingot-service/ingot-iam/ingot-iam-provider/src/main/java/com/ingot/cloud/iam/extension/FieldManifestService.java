package com.ingot.cloud.iam.extension;

import java.util.Map;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.authorization.*;
import com.ingot.framework.authorization.field.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.ingot.framework.cache.spi.LayeredCache;

/**
 * <p>本地注解清单或服务器白名单内远程服务清单；远端请求使用独立服务签名。</p>
 * @author jy
 * @since 1.0.0
 */
@Service @RequiredArgsConstructor
public class FieldManifestService implements InitializingBean {
    private final FieldBindingRegistry fields;
    private final ResourceRpcProperties properties;
    private final ApplicationContext context;
    private final ObjectMapper mapper;
    private final ObjectProvider<LayeredCache<String, FieldBindingManifest>> cache;
    private final Map<ResourceKey, Remote> remotes = new LinkedHashMap<>();

    @Override
    public void afterPropertiesSet() {
        for (var registration : properties.getRegistrations()) {
            if (registration.getDescriptor() == null || registration.getServiceName() == null
                    || !registration.getServiceName().matches("[a-zA-Z][a-zA-Z0-9-]*"))
                throw new IllegalArgumentException("清单资源必须绑定固定服务发现名称");
            var rpc = new FeignClientBuilder(context).forType(FieldManifestRpc.class, registration.getServiceName()).build();
            if (remotes.putIfAbsent(registration.getDescriptor().key(), new Remote(rpc, registration.getManifestSecret())) != null)
                throw new IllegalArgumentException("清单资源重复注册");
        }
    }

    /** 本地无需 IO；远程仅复用最长 30 秒的元数据，不借用用户令牌。 */
    public FieldBindingManifest require(ResourceKey resource) {
        if (!remotes.containsKey(resource))
            return fields.manifest(resource);
        var configured = cache.getIfAvailable();
        return configured == null ? load(resource) : configured.get(json(resource));
    }

    /** 缓存 loader 或显式启动预热读取服务器清单，失败不能返回假空清单。 */
    public FieldBindingManifest load(ResourceKey resource) {
        if (!remotes.containsKey(resource))
            return fields.manifest(resource);
        var remote = remotes.get(resource);
        try {
            var payload = json(new FieldManifestInvocation(FieldManifestInvocation.PURPOSE,
                    FieldManifestInvocation.IAM_CALLER, resource));
            var response = remote.rpc().manifest(ResourceRpcSigner.sign(payload, remote.secret()));
            if (response == null || !response.isSuccess() || response.getData() == null
                    || !resource.equals(response.getData().resource()) || response.getData().version() == null)
                throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            return response.getData();
        }
        catch (Exception failure) { throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE); }
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (java.io.IOException failure) { throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE); }
    }

    /** 服务端固定发现目标与专用清单密钥。 */
    private record Remote(FieldManifestRpc rpc, String secret) { }
}
