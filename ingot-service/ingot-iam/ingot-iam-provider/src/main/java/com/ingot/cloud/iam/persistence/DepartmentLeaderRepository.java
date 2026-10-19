package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentLeaderMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.projection.DepartmentLeaderRow;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>负责人关系持久化边界，不查询角色或根据负责人扩展授权。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class DepartmentLeaderRepository {
    private final IamDepartmentLeaderMapper mapper;
    private final IamTenantMemberMapper members;

    /** 按关联或候选 SQL 分页。 */
    public List<DepartmentLeaderRow> page(DepartmentLeaderSql.Query query) { return mapper.page(query); }

    /** 按分页相同条件统计。 */
    public long count(DepartmentLeaderSql.Query query) { return mapper.count(query); }

    /** 对可信的部门集合批量汇总，不逐部门查询。 */
    public List<DepartmentLeaderRow> summaries(DepartmentLeaderSql.Query query) {
        return query.departmentIds().isEmpty() ? List.of() : mapper.summaries(query);
    }

    /** 部门持锁后读取事务校验所需的完整关系。 */
    public List<DepartmentLeaderRow> lockRelations(BigInteger tenant, BigInteger department) {
        return mapper.lockRelations(tenant, department);
    }

    /** 锁定当前租户成员和关联账号。 */
    public DepartmentLeaderRow lockMember(BigInteger tenant, BigInteger member) {
        return mapper.lockMember(tenant, member, MemberStatus.ACTIVE.getValue());
    }

    /** 包含已移出历史成员的范围检查，供显式解除失效关系使用。 */
    public boolean visibleMember(BigInteger tenant, BigInteger member, ObjectScope scope) {
        var query = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, tenant).eq(IamTenantMemberEntity::getId, member);
        ObjectScopeSql.restrictTenantMembers(query, scope, tenant);
        return members.selectCount(query) > 0;
    }

    /** 检查部门是否仍有负责人，含不可用历史关系。 */
    public long relationCount(long tenant, long department) {
        return mapper.relationCount(BigInteger.valueOf(tenant), BigInteger.valueOf(department));
    }

    /** 已持有组织及部门锁时原子保存差量和唯一主负责人，递增部门版本。 */
    public void save(BigInteger tenant, BigInteger department, Map<BigInteger, Boolean> before,
                     Map<BigInteger, Boolean> after, BigInteger version) {
        save(tenant, department, before, after, version, true);
    }

    /** 统一编辑事务可延后版本递增，其他关系校验不变。 */
    public void save(BigInteger tenant, BigInteger department, Map<BigInteger, Boolean> before,
                     Map<BigInteger, Boolean> after, BigInteger version, boolean advanceVersion) {
        mapper.clearPrimary(tenant, department);
        before.keySet().stream().filter(id -> !after.containsKey(id)).forEach(id -> mapper.remove(tenant, department, id));
        after.keySet().stream().filter(id -> !before.containsKey(id)).forEach(id -> mapper.add(tenant, department, id));
        after.forEach((id, primary) -> { if (primary) mapper.primary(tenant, department, id); });
        if (advanceVersion && mapper.incrementVersion(tenant, department, version) != 1) {
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
        }
    }
}
