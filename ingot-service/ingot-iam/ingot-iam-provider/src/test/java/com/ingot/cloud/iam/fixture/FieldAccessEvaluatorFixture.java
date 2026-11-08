package com.ingot.cloud.iam.fixture;



import com.ingot.cloud.iam.infrastructure.persistence.DepartmentClosure;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberDepartmentMapper;


import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.application.policy.FieldPolicySnapshotLoader;
import com.ingot.cloud.iam.application.policy.FieldPolicySnapshot;
import com.ingot.cloud.iam.application.policy.FieldDefaultReference;
import com.ingot.cloud.iam.application.policy.MemberFieldProjector;
import com.ingot.cloud.iam.infrastructure.persistence.FieldPolicyRepository;

/**
 * <p>测试中装配字段快照、求值与成员投影，生产代码只保留职责明确的构造依赖。</p>
 * @author jy
 * @since 1.0.0
 */
public final class FieldAccessEvaluatorFixture {
    private FieldAccessEvaluatorFixture() { }

    /** 用测试夹具依赖装配字段策略协作者。 */
    public static FieldAccessEvaluator create(FieldPolicyRepository policies,
            IamMemberDepartmentMapper memberships, DepartmentClosure closures,
            org.springframework.beans.factory.ObjectProvider<com.ingot.framework.cache.spi.LayeredCache<String, FieldPolicySnapshot>> cache,
            com.ingot.framework.authorization.field.FieldProjectionEngine projection,
            com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata metadata,
            org.springframework.beans.factory.ObjectProvider<com.ingot.framework.cache.spi.LayeredCache<String, FieldDefaultReference>> defaults) {
        var snapshots = new FieldPolicySnapshotLoader(policies, metadata, defaults);
        var projector = new MemberFieldProjector(projection);
        return new FieldAccessEvaluator(policies, memberships, closures, metadata, defaults, snapshots, projector);
    }
}
