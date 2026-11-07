package com.ingot.cloud.iam.application.field;

import com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.application.authorization.extension.FieldCachePrewarmer;
import com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata;
import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.application.policy.FieldDefaultReference;
import com.ingot.cloud.iam.application.policy.FieldPolicySnapshot;
import com.ingot.cloud.iam.application.policy.PolicyWriteRepository;
import com.ingot.cloud.iam.configuration.FieldPolicyCacheConfiguration;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldOperations;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.MaskSpec;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.Selection;

import com.ingot.cloud.iam.domain.organization.MemberResources;

import java.util.*;
import java.util.stream.LongStream;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ingot.cloud.iam.domain.authorization.AuthorizationChangedSpringEvent;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentClosure;
import com.ingot.cloud.iam.infrastructure.persistence.FieldTestSupport;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.framework.authorization.field.FieldBindingRegistry;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.model.iam.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * <p>字段批次不按行增加数据库查询，预热范围与本地失效保持公共缓存契约。</p>
 * @author jy
 * @since 1.0.0
 */
class FieldCacheAndBatchTest {
    @Test
    void oneTwentyAndHundredRowsEachLoadRelationshipsOnce() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "field-test"), IamMemberDepartmentEntity.class);
        var memberships = mock(IamMemberDepartmentMapper.class);
        when(memberships.selectList(any())).thenReturn(List.of());
        var evaluator = new FieldAccessEvaluator(mock(PolicyWriteRepository.class), memberships, mock(DepartmentClosure.class),
                FieldTestSupport.noCache(), FieldTestSupport.projection(), FieldTestSupport.metadata(), FieldTestSupport.noCache());
        var full = new FieldAccess(FieldVisibility.FULL, true);
        var snapshot = new FieldPolicySnapshot(10, BuiltinResourceProviders.TENANT_MEMBER_RESOURCE, Map.of("phone", full), Map.of("phone", full),
                List.of(new FieldAccessEvaluator.FieldRuleRow(new Selection(List.of("1"), List.of()),
                        List.of(new ScopeExpression(ScopeKind.ALL, null, null)), Map.of(), "phone", FieldVisibility.FULL)),
                List.of(), Map.of("phone", new FieldOperations(true, false)), Map.of("phone", new FieldOperations(true, false)),
                Map.of("phone", MaskSpec.PHONE));
        for (int count : List.of(1, 20, 100)) {
            clearInvocations(memberships);
            var ids = LongStream.rangeClosed(2, count + 1L).boxed().toList();
            var batch = evaluator.prepare(snapshot, 1, ids);
            for (long id : ids) assertEquals(FieldVisibility.FULL, evaluator.memberAccess(batch, id).get("phone").visibility());
            verify(memberships, times(1)).selectList(any());
            verifyNoMoreInteractions(memberships);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void prewarmOnlyOwnResourcesAndPublicReferenceAndInvalidationClearsBothCaches() {
        var bindings = mock(FieldBindingRegistry.class);
        var metadata = mock(ResourceFieldMetadata.class);
        when(bindings.resources()).thenReturn(Set.of(MemberResources.PLATFORM_MEMBER, MemberResources.TENANT_MEMBER));
        when(metadata.require(MemberResources.TENANT_MEMBER, false))
                .thenReturn(new ResourceFieldMetadata.Entry("10", null));
        LayeredCache<String, FieldDefaultReference> defaults = mock(LayeredCache.class);
        LayeredCache<String, FieldPolicySnapshot> policy = mock(LayeredCache.class);
        new FieldCachePrewarmer(bindings, metadata, defaults).ready();
        verify(metadata).require(MemberResources.PLATFORM_MEMBER, false);
        verify(metadata, times(2)).require(MemberResources.TENANT_MEMBER, false);
        verify(defaults).get(FieldPolicyCacheConfiguration.latest(10));
        verifyNoMoreInteractions(metadata, defaults);
        clearInvocations(defaults);
        new FieldPolicyCacheConfiguration.OriginInvalidator(policy, defaults).changed(AuthorizationChangedSpringEvent.all(this));
        verify(policy).evictAll(); verify(defaults).evictAll();
    }
}
