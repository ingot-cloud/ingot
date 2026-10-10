package com.ingot.framework.authorization.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.*;
import com.ingot.framework.commons.model.support.*;
import com.ingot.framework.security.config.annotation.web.configuration.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>本服务的纯元数据端点，服务签名与 INNER 边界共同校验，无用户上下文依赖。</p>
 * @author jy
 * @since 1.0.0
 */
@RestController @Permit(mode = PermitMode.INNER) @RequiredArgsConstructor
public final class FieldManifestEndpoint implements RShortcuts {
    private final FieldBindingRegistry fields;
    private final ResourceRegistry resources;
    private final ObjectMapper mapper;
    private final FieldManifestProperties properties;

    /** 仅返回完整资源的编译清单，不提供 SQL、业务原值或任意服务调用能力。 */
    @PostMapping(FieldManifestRpc.PATH)
    public R<FieldBindingManifest> manifest(@RequestBody SignedResourceObjectRequest signed) {
        ResourceRpcSigner.verify(signed, properties.getSecret());
        FieldManifestInvocation request;
        try { request = mapper.readValue(signed.payload(), FieldManifestInvocation.class); }
        catch (java.io.IOException exception) { throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT); }
        if (request == null || !FieldManifestInvocation.PURPOSE.equals(request.purpose())
                || !properties.getCallers().contains(request.callerService()) || request.resource() == null)
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        resources.require(request.resource());
        return ok(fields.manifest(request.resource()));
    }
}
