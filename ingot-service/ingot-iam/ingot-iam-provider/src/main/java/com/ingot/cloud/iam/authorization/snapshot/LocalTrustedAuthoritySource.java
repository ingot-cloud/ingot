package com.ingot.cloud.iam.authorization.snapshot;

import java.util.LinkedHashSet;
import java.util.Set;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * <p>IAM 接口注解的在线授权，先重验身份，再读取数据库有效来源。</p>
 * @author jy
 * @since 1.0.0
 */
@Primary
@Service
@RequiredArgsConstructor
public class LocalTrustedAuthoritySource implements TrustedAuthoritySource {
    private final CurrentIdentityService identities;
    private final AuthorizationEvaluator evaluator;

    /** {@inheritDoc} */
    @Override
    public Set<String> currentAuthorities() {
        var actor = identities.requireCurrent();
        var view = evaluator.evaluateForExecution(actor.context(), true);
        Set<String> result = new LinkedHashSet<>(view.actionCodes());
        result.remove(RoleConstants.ROLE_ADMIN_CODE);
        if (actor.context().domain() == com.ingot.framework.commons.model.iam.AuthorizationDomain.PLATFORM
                && view.platformAdministrator()) result.add(RoleConstants.ROLE_ADMIN_CODE);
        return Set.copyOf(result);
    }
}
