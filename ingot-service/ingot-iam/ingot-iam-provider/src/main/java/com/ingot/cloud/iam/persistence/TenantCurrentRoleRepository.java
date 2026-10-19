package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.*;
import com.ingot.cloud.iam.persistence.mapper.*;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>保存租户完整内部快照并切换当前指针，平台固定版本持久化不改变。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class TenantCurrentRoleRepository {
    private final TenantLiveMapper live;
    private final IamRoleDefinitionMapper definitions;
    private final IamRoleRevisionMapper revisions;
    private final IamRoleGrantMapper grants;
    private final IamRoleParameterMapper parameters;
    private final IamRoleDeltaMapper deltas;

    /** 插入角色身份，完整当前快照在同一事务中建立。 */
    public void insert(long id, Long tenantId, RoleKind kind, CurrentRoleCreateInput input) {
        var row = new IamRoleDefinitionEntity();
        row.setId(BigInteger.valueOf(id));
        row.setDomain(AuthorizationDomain.TENANT);
        row.setTenantId(tenantId == null ? null : BigInteger.valueOf(tenantId));
        row.setKind(kind);
        row.setCode(input.code().trim());
        row.setName(input.name().trim());
        row.setDescription(input.description());
        row.setGroupName(input.groupName());
        row.setEnabled(true);
        definitions.insert(row);
    }

    /** 插入无基础引用的不可变内部快照，并原子切换当前权限。 */
    public boolean save(TenantLiveRows.Role role, long snapshotId, CurrentRoleDefinition definition) {
        var row = new IamRoleRevisionEntity();
        row.setId(BigInteger.valueOf(snapshotId));
        row.setRoleId(role.id());
        row.setKind(role.kind());
        row.setRevision(live.nextRevision(role.id()));
        row.setMetadataOverrides(IamJson.object(null));
        row.setResourceFieldPermissions(IamJson.object(null));
        row.setMetadataVersion(BigInteger.ZERO);
        row.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        revisions.insert(row);
        for (var grant : definition.grants()) {
            var item = new IamRoleGrantEntity();
            item.setRevisionId(row.getId());
            item.setActionId(BigInteger.valueOf(IamIds.require(grant.actionId())));
            item.setScopes(IamJson.array(grant.scopes()));
            grants.insert(item);
        }
        for (var parameter : definition.parameterDefinitions()) {
            var item = new IamRoleParameterEntity();
            item.setRevisionId(row.getId());
            item.setParameterKey(parameter.key());
            item.setBindingKind(parameter.kind());
            parameters.insert(item);
            live.parameterResource(row.getId(), parameter.key(), new BigInteger(parameter.resourceId()));
        }
        return live.current(role.id(), row.getId(), role.version()) == 1;
    }

    /** 读取当前完整权限，缺少可信当前指针时明确失败关闭。 */
    public CurrentRoleDefinition definition(TenantLiveRows.Role role) {
        if (role.currentRevisionId() == null) {
            throw new com.ingot.framework.commons.error.BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        List<ActionGrant> values = grants.listByRevision(role.currentRevisionId()).stream()
                .map(row -> new ActionGrant(row.actionId().toString(),
                        IamJson.read(row.scopes(), new com.fasterxml.jackson.core.type.TypeReference<List<ScopeExpression>>() { })))
                .toList();
        List<CurrentRoleParameter> definitions = live.parameters(role.currentRevisionId()).stream()
                .map(row -> new CurrentRoleParameter(row.key(), row.kind(), row.resourceId().toString())).toList();
        return new CurrentRoleDefinition(values, definitions);
    }

    /** 当前请求批量读取不可变定义，避免按分配逐角色查询。 */
    public Map<BigInteger,CurrentRoleDefinition> definitions(List<TenantLiveRows.Role> heads) {
        if(heads.isEmpty())return Map.of();
        if(heads.stream().anyMatch(role->role.currentRevisionId()==null))throw new com.ingot.framework.commons.error.BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        var ids=heads.stream().map(TenantLiveRows.Role::currentRevisionId).distinct().toList();
        var granted=new HashMap<BigInteger,List<ActionGrant>>();
        var parameters=new HashMap<BigInteger,List<CurrentRoleParameter>>();
        var parsed=new HashMap<String,List<ScopeExpression>>();
        for(var item:live.snapshotGrants(ids))granted.computeIfAbsent(item.snapshotId(),key->new ArrayList<>()).add(new ActionGrant(item.actionId().toString(),parsed.computeIfAbsent(item.scopes(),json->IamJson.read(json,new com.fasterxml.jackson.core.type.TypeReference<List<ScopeExpression>>() { }))));
        for(var item:live.snapshotParameters(ids))parameters.computeIfAbsent(item.snapshotId(),key->new ArrayList<>()).add(new CurrentRoleParameter(item.key(),item.kind(),item.resourceId().toString()));
        var result=new LinkedHashMap<BigInteger,CurrentRoleDefinition>();
        heads.forEach(role->result.put(role.id(),new CurrentRoleDefinition(granted.getOrDefault(role.currentRevisionId(),List.of()),parameters.getOrDefault(role.currentRevisionId(),List.of()))));
        return result;
    }

    /** 删除未被实际外部引用的角色，历史审计行不清理。 */
    public void delete(TenantLiveRows.Role role) {
        live.clearCurrent(role.id());
        var rows = revisions.selectList(Wrappers.<IamRoleRevisionEntity>lambdaQuery()
                .eq(IamRoleRevisionEntity::getRoleId, role.id()));
        for (var row : rows) {
            grants.delete(Wrappers.<IamRoleGrantEntity>lambdaQuery().eq(IamRoleGrantEntity::getRevisionId, row.getId()));
            parameters.delete(Wrappers.<IamRoleParameterEntity>lambdaQuery().eq(IamRoleParameterEntity::getRevisionId, row.getId()));
            deltas.delete(Wrappers.<IamRoleDeltaEntity>lambdaQuery().eq(IamRoleDeltaEntity::getRevisionId, row.getId()));
        }
        revisions.delete(Wrappers.<IamRoleRevisionEntity>lambdaQuery().eq(IamRoleRevisionEntity::getRoleId, role.id()));
        definitions.deleteById(role.id());
    }
}
