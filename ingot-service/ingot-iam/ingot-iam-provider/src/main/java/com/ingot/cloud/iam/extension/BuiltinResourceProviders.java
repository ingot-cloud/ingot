package com.ingot.cloud.iam.extension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.ingot.cloud.iam.assignment.PlatformScopeObjectResource;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.authorization.ResourceRegistry;
import com.ingot.framework.commons.model.iam.AuthorizationCandidateKind;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.AuthorizationOption;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.ObjectAssociationKind;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 内置对象查询的私有适配实现；公共解析仅使用完整注册键。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class BuiltinResourceProviders {

    /**
     * 内置平台应用的服务器命名空间。
     */
    public static final String PLATFORM_APPLICATION = "iam-platform";

    private final AuthorizationCandidateMapper candidates;

    /**
     * 装配内置与额外业务provider。
     * @param extensions 业务扩展
     * @return 完整注册表
     */
    public ResourceRegistry registry(List<ResourceObjectProvider> extensions) {
        List<ResourceObjectProvider> all = new ArrayList<>(extensions);
        Map<ResourceKey, List<ActionDescriptor>> actions = new LinkedHashMap<>();
        for (var action : IamAction.values()) {
            String[] parts = action.getCode().split(":");
            var domain = parts[0].equals(PLATFORM_APPLICATION) ? AuthorizationDomain.PLATFORM
                    : AuthorizationDomain.TENANT;
            var key = new ResourceKey(domain, parts[0], parts[1]);
            actions.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new ActionDescriptor(action.getCode(),
                        action.getOperation().isMutating() ? ExecutionMode.MUTATING : ExecutionMode.READ_ONLY));
        }
        actions.forEach((key, operations) -> all.add(new BuiltinProvider(descriptor(key, operations),
                key.domain() == AuthorizationDomain.PLATFORM ? objectResource(key.resourceCode()) : null)));
        return new ResourceRegistry(all);
    }

    private static PlatformScopeObjectResource objectResource(String code) {
        return PlatformScopeObjectResource.find(code);
    }

    private static ResourceDescriptor descriptor(ResourceKey key, List<ActionDescriptor> actions) {
        var object = key.domain() == AuthorizationDomain.PLATFORM ? objectResource(key.resourceCode()) : null;
        List<FieldCapability> fields = new ArrayList<>();
        Map<String, FieldAccess> defaults = new LinkedHashMap<>();
        if (key.domain() == AuthorizationDomain.PLATFORM
                && key.resourceCode().equals(PlatformScopeObjectResource.MEMBER.getValue())) {
            for (var field : MemberFieldKey.values()) {
                fields.add(new FieldCapability(field.getValue(), fieldLabel(field), List.of(FieldVisibility.values()),
                        true, field == MemberFieldKey.DISPLAY_NAME, false));
                defaults.put(field.getValue(),
                        new FieldAccess(
                                field == MemberFieldKey.PHONE || field == MemberFieldKey.EMAIL ? FieldVisibility.MASKED
                                        : FieldVisibility.FULL,
                                field != MemberFieldKey.PHONE && field != MemberFieldKey.EMAIL));
            }
        }
        String read = object == null ? actions.getFirst().code() : object.getReadAction().getCode();
        return new ResourceDescriptor(key, actions,
                object == null ? List.of(ScopeKind.ALL)
                        : key.resourceCode().equals(PlatformScopeObjectResource.MEMBER.getValue())
                                ? List.of(ScopeKind.ALL, ScopeKind.SELF, ScopeKind.OBJECT_SET)
                                : List.of(ScopeKind.ALL, ScopeKind.OBJECT_SET),
                fields, defaults, read, object == PlatformScopeObjectResource.MENU);
    }

    private static String fieldLabel(MemberFieldKey field) {
        return switch (field) {
            case DISPLAY_NAME -> "显示名称";
            case AVATAR -> "头像";
            case PHONE -> "手机号";
            case EMAIL -> "邮箱";
        };
    }

    @RequiredArgsConstructor
    private final class BuiltinProvider implements ResourceObjectProvider {

        private final ResourceDescriptor descriptor;

        private final PlatformScopeObjectResource object;

        @Override
        public ResourceDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public boolean nativeAssociations() {
            return true;
        }

        @Override
        public AuthorizationCandidatePage candidates(ResourceObjectQuery input) {
            if (object == null)
                return new AuthorizationCandidatePage(List.of(), 0, input.page(), input.pageSize(), false,
                        "该资源未接入范围对象查询");
            IamPages.require(input.page(), input.pageSize());
            List<BigInteger> ids = numbers(input.ids()),
                    allowed = input.allowedIds() == null ? null : numbers(input.allowedIds());
            String search = input.keyword() == null ? "" : input.keyword().trim();
            search = "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            boolean tree = descriptor.hierarchical() && input.tree();
            var navigation = allowed;
            if (tree && ids.isEmpty() && "%".equals(search) && allowed != null && !allowed.isEmpty())
                navigation = candidates.menuAncestorIds(allowed);
            var relation = input.association();
            var query = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT,
                    input.context() == null ? null : new BigInteger(input.context().memberId()), null, null,
                    object.getValue(), search, ids, navigation, Math.multiplyExact(input.page() - 1, input.pageSize()),
                    input.pageSize(), tree && relation == null,
                    input.parentId() == null ? null : new BigInteger(input.parentId()), null,
                    relation != null && relation.kind() == ObjectAssociationKind.DELEGATION
                            ? new BigInteger(relation.id()) : null,
                    relation == null || relation.actionId() == null ? null : new BigInteger(relation.actionId()),
                    relation != null && relation.kind() == ObjectAssociationKind.ASSIGNMENT
                            ? new BigInteger(relation.id()) : null,
                    relation == null ? null : relation.parameterKey());
            var rows = candidates.page(query);
            Map<BigInteger, String> paths = descriptor.hierarchical() && !rows.isEmpty()
                    ? candidates.menuPaths(rows.stream().map(r -> r.id()).toList())
                        .stream()
                        .collect(Collectors.toMap(AuthorizationCandidateMapper.TreePath::leafId,
                                AuthorizationCandidateMapper.TreePath::ancestorPath))
                    : Map.of();
            var selectable = allowed;
            var options = rows.stream()
                .map(row -> new AuthorizationOption(row.id().toString(), row.name(), null, null, null, null, null, null,
                        row.parentId() == null ? null : row.parentId().toString(), row.hasChildren(),
                        paths.get(row.id()),
                        !descriptor.hierarchical() || selectable == null || selectable.contains(row.id())))
                .toList();
            return new AuthorizationCandidatePage(options, candidates.count(query), input.page(), input.pageSize(),
                    true, null, null, descriptor.hierarchical());
        }

        @Override
        public boolean objectsExist(AuthorizationContext context, List<String> values) {
            if (object == null)
                return false;
            if (values.isEmpty())
                return true;
            var ids = numbers(values);
            var query = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, null, null, null,
                    object.getValue(), "%", ids, null, 0, IamPages.DEFAULT_SIZE);
            return candidates.count(query) == ids.size();
        }

        private List<BigInteger> numbers(List<String> values) {
            return values.stream().map(v -> BigInteger.valueOf(IamIds.require(v))).distinct().toList();
        }

    }

}
