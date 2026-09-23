package com.ingot.cloud.iam.organization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.identity.InitializationIdAllocator;
import com.ingot.cloud.iam.persistence.MemberLifecycleRepository;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberDepartmentInput;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatusInput;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.VersionInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>把受保护 HTTP 接到成员生命周期事务，使用生产 Guard 而不是空实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberCommandService {
    private final CurrentIdentityService current;
    private final MemberLifecycle lifecycle;
    private final MemberMutationGuard guard;
    private final InitializationIdAllocator ids;
    private final MemberLifecycleRepository members;

    /**
     * 暂停或恢复指定管理域中的成员资格。
     *
     * @param domain 接口声明的管理域，不能取自客户端
     * @param memberId 路径中的成员 ID
     * @param input 状态与预期版本
     * @return 提交后的成员 ID 与版本
     */
    public CreatedResource changeStatus(AuthorizationDomain domain, String memberId, MemberStatusInput input) {
        ActiveIdentity actor = current.requireDomain(domain);
        String version = lifecycle.changeStatus(actor.context(), memberId, input, ids.nextId(), guard);
        return new CreatedResource(memberId, version);
    }

    /**
     * 移出指定管理域中的成员，不删除账号或其他身份。
     *
     * @param domain 接口声明的管理域，不能取自客户端
     * @param memberId 路径中的成员 ID
     * @param input 预期版本
     * @return 提交后的成员 ID 与版本
     */
    public CreatedResource remove(AuthorizationDomain domain, String memberId, VersionInput input) {
        ActiveIdentity actor = current.requireDomain(domain);
        String version = lifecycle.remove(actor.context(), memberId, input, ids.nextId(), guard);
        return new CreatedResource(memberId, version);
    }

    /**
     * 整体替换租户任职并返回不含凭证与隐藏字段的投影。
     *
     * @param memberId 当前租户成员
     * @param input 目标关系
     * @return 字段策略接入前的安全投影，不含手机号邮箱原值
     */
    public ResourceDetail<MemberRecord> replaceDepartments(String memberId, MemberDepartmentInput input) {
        ActiveIdentity actor = current.requireDomain(AuthorizationDomain.TENANT);
        List<MemberDepartmentPlan.Department> departments = new ArrayList<>();
        for (MemberDepartmentBinding binding : input.departments()) {
            departments.add(new MemberDepartmentPlan.Department(binding.id(), binding.primary()));
        }
        String version = lifecycle.replaceDepartments(actor.context(), memberId,
                new MemberDepartmentPlan(input.expectedVersion(), departments), ids.nextId(), guard);
        return projectTenantMember(actor, memberId, version);
    }

    private ResourceDetail<MemberRecord> projectTenantMember(ActiveIdentity actor, String memberId, String version) {
        long tenantId = Long.parseLong(actor.context().tenantId());
        IamTenantMemberEntity member = members.findTenantMember(tenantId, Long.parseLong(memberId));
        if (member == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        MemberRecord record = new MemberRecord(member.getId().toString(), member.getDisplayName(), null, null, null,
                null, member.getStatus(), members.departmentViews(tenantId, Long.parseLong(memberId)));
        return new ResourceDetail<>(record, Map.of(), Map.of(), version);
    }
}
