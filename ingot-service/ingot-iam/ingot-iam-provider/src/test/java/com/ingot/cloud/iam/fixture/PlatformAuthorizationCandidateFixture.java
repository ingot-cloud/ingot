package com.ingot.cloud.iam.fixture;

import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.framework.authorization.ResourceRegistry;
import java.util.*;
import com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository;
import com.ingot.cloud.iam.infrastructure.persistence.DelegationRepository;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.authorization.*;
import com.ingot.cloud.iam.application.authorization.candidate.*;

/**
 * <p>装配候选查询回归所需的真实对象解析服务。</p>
 * @author jy
 * @since 1.0.0
 */
public final class PlatformAuthorizationCandidateFixture {
    private PlatformAuthorizationCandidateFixture() { }
    /** 使用既有依赖构造查询服务。 */
    public static PlatformAuthorizationCandidateQueryService create(IamAccess access, com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator evaluator, AssignmentRepository assignments, DelegationRepository delegations, RoleRepository roleStore, PlatformRoleFacade roles, AuthorizationCandidateMapper candidates, ResourceRegistry registry) {
        var objects=new PlatformScopeObjectResolver(assignments,roles,candidates,registry);
        return new PlatformAuthorizationCandidateQueryService(objects, access, evaluator, assignments, delegations, roleStore, roles, candidates);
    }
}
