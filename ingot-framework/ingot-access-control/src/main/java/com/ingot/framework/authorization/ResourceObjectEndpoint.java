package com.ingot.framework.authorization;

import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectInvocation;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectResult;
import com.ingot.framework.commons.model.iam.extension.SignedResourceObjectRequest;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>资源服务内部对象入口；签名证明IAM调用方，在线认证证明原始成员，两者都必须成立。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Permit(mode = PermitMode.INNER)
public class ResourceObjectEndpoint implements RShortcuts {

    private static final int MAX_PAGE_SIZE = 200;

    private final ResourceRegistry registry;

    private final ObjectMapper mapper;

    private final String secret;

    /**
     * 装配资源端点。
     * @param registry 本服务资源
     * @param mapper JSON
     * @param secret IAM专用共享密钥
     */
    public ResourceObjectEndpoint(ResourceRegistry registry, ObjectMapper mapper, String secret) {
        this.registry = registry;
        this.mapper = mapper;
        this.secret = secret;
    }

    /**
     * 查询已授权的最小候选或存在性。
     * @param request 签名内容
     * @return 最小结果
     */
    @PostMapping(ResourceObjectRpc.PATH)
    public R<ResourceObjectResult> query(@RequestBody SignedResourceObjectRequest request) {
        ResourceRpcSigner.verify(request, secret);
        ResourceObjectInvocation invocation;
        try {
            invocation = mapper.readValue(request.payload(), ResourceObjectInvocation.class);
        }
        catch (Exception exception) {
            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        }
        var query = invocation == null ? null : invocation.query();
        if (query == null || query.resource() == null || query.context() == null) {
            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (!Objects.equals(query.context(), RemoteAuthorizationClient.current())) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
        if (query.resource().domain() != query.context().domain() || query.page() < 1 || query.pageSize() < 1
                || query.pageSize() > MAX_PAGE_SIZE || query.ids().size() > MAX_PAGE_SIZE) {
            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        }
        var provider = registry.require(query.resource());
        if (!invocation.verifyIds().isEmpty()) {
            return ok(new ResourceObjectResult(null, provider.objectsExist(query.context(), invocation.verifyIds())));
        }
        return ok(new ResourceObjectResult(provider.candidates(query), null));
    }

}
