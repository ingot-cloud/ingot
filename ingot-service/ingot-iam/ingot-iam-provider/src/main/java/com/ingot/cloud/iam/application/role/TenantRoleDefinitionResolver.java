package com.ingot.cloud.iam.application.role;

import com.ingot.cloud.iam.infrastructure.persistence.TenantRoleDefinitionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.cloud.iam.domain.role.CurrentRoleDefinition;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.TenantAdministrationRows;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按当前开通和操作状态解析租户可执行角色定义，供分配与委派共同使用。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantRoleDefinitionResolver {

    private final TenantAdministrationMapper live;
    private final TenantRoleDefinitionRepository roles;

    /** 分配仅配置本组织当前可执行资源；未开通或停用资源不制造不可见必填参数。 */
    public CurrentRoleDefinition activeDefinition(ActiveIdentity actor,TenantAdministrationRows.Role role) {
        return active(actor, Map.of(role.id(), roles.definition(role))).definitions().get(role.id());
    }

    /**
     * 读取当前分页角色头对应的定义，并一次应用当前租户的开通与操作状态。
     * @param actor 当前可信租户身份
     * @param heads 已校验当前租户归属的角色头
     * @return 按角色 ID 的可执行定义与操作资源
     */
    public ActiveDefinitions activeDefinitions(ActiveIdentity actor, List<TenantAdministrationRows.Role> heads) {
        return active(actor, roles.definitions(heads));
    }

    private ActiveDefinitions active(ActiveIdentity actor, Map<BigInteger, CurrentRoleDefinition> definitions) {
        var actionIds = definitions.values().stream().flatMap(definition -> definition.grants().stream())
                .map(grant -> new BigInteger(grant.actionId())).distinct().toList();
        if (actionIds.isEmpty()) return new ActiveDefinitions(definitions, Map.of());
        var opened = live.applications(tenant(actor)).stream().map(TenantAdministrationRows.Application::id)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, BigInteger> resources = new HashMap<>();
        live.actions(actionIds).stream()
                .filter(action -> Boolean.TRUE.equals(action.enabled()) && opened.contains(action.applicationId()))
                .forEach(action -> resources.put(action.actionId().toString(), action.resourceId()));
        Map<BigInteger, CurrentRoleDefinition> active = new HashMap<>();
        definitions.forEach((id, definition) -> {
            if (definition.grants().isEmpty()) {
                active.put(id, definition);
                return;
            }
            var grants = definition.grants().stream().filter(grant -> resources.containsKey(grant.actionId())).toList();
            var keys = grants.stream().flatMap(grant -> grant.scopes().stream()).map(ScopeExpression::parameterKey)
                    .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            active.put(id, new CurrentRoleDefinition(grants,
                    definition.parameterDefinitions().stream().filter(parameter -> keys.contains(parameter.key())).toList(),
                    definition.resourceFieldPermissions()));
        });
        return new ActiveDefinitions(active, resources);
    }

    /**
     * 当前页角色的可执行权限及其操作资源映射。
     * @author jy
     * @since 1.0.0
     */
    public record ActiveDefinitions(Map<BigInteger, CurrentRoleDefinition> definitions,
                                    Map<String, BigInteger> actionResources) { }

    private static BigInteger tenant(ActiveIdentity a){return new BigInteger(a.context().tenantId());}
}
