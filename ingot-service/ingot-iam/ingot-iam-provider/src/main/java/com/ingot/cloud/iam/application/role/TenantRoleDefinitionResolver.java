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
        var definition=roles.definition(role);
        if(definition.grants().isEmpty())return definition;
        var opened=live.applications(tenant(actor)).stream().map(TenantAdministrationRows.Application::id).collect(java.util.stream.Collectors.toSet());
        var ids=live.actions(definition.grants().stream().map(g->new BigInteger(g.actionId())).toList()).stream()
            .filter(a->Boolean.TRUE.equals(a.enabled()) && opened.contains(a.applicationId())).map(a->a.actionId().toString()).collect(java.util.stream.Collectors.toSet());
        var grants=definition.grants().stream().filter(g->ids.contains(g.actionId())).toList();
        var keys=grants.stream().flatMap(g->g.scopes().stream()).map(ScopeExpression::parameterKey).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        return new CurrentRoleDefinition(grants,definition.parameterDefinitions().stream().filter(p->keys.contains(p.key())).toList(),definition.resourceFieldPermissions());
    }
    private static BigInteger tenant(ActiveIdentity a){return new BigInteger(a.context().tenantId());}
}
