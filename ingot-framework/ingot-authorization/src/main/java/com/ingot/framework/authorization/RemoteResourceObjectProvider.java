package com.ingot.framework.authorization;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.ObjectQueryPurpose;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectInvocation;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectResult;

/**
 * <p>目录侧远程适配器，仅调用预注册资源服务；不从请求构造URL。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class RemoteResourceObjectProvider implements ResourceObjectProvider {

    private final ResourceDescriptor descriptor;

    private final ResourceObjectRpc remote;

    private final String secret;

    private final ObjectMapper mapper;

    /**
     * 装配白名单适配器。
     * @param descriptor 可信资源能力
     * @param remote 固定服务客户端
     * @param secret 专用密钥
     * @param mapper JSON
     */
    public RemoteResourceObjectProvider(ResourceDescriptor descriptor, ResourceObjectRpc remote, String secret,
            ObjectMapper mapper) {
        this.descriptor = descriptor;
        this.remote = remote;
        this.secret = secret;
        this.mapper = mapper;
        ResourceRpcSigner.sign("", secret);
    }

    @Override
    public ResourceDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public AuthorizationCandidatePage candidates(ResourceObjectQuery query) {
        var value = invoke(new ResourceObjectInvocation(query, List.of()));
        if (value.candidates() == null)
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        return value.candidates();
    }

    @Override
    public boolean objectsExist(AuthorizationContext context, List<String> ids) {
        if (ids == null || ids.isEmpty())
            return false;
        var query = new ResourceObjectQuery(descriptor.key(), context, ObjectQueryPurpose.ASSIGNMENT, null, List.of(),
                null, 1, 1, false, null, null);
        var value = invoke(new ResourceObjectInvocation(query, ids));
        if (value.exists() == null)
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        return value.exists();
    }

    private ResourceObjectResult invoke(ResourceObjectInvocation request) {
        if (!descriptor.key().equals(request.query().resource()))
            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        try {
            var result = remote.query(ResourceRpcSigner.sign(mapper.writeValueAsString(request), secret));
            if (result == null || !result.isSuccess() || result.getData() == null)
                throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            return result.getData();
        }
        catch (BizException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
    }

}
