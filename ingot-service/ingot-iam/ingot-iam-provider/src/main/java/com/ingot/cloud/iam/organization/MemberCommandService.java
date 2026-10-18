package com.ingot.cloud.iam.organization;

import java.util.ArrayList;
import java.util.List;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.identity.InitializationIdAllocator;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
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
    private final MemberQueryService queries;

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
     * @return 与成员详情相同的字段投影
     */
    public ResourceDetail<MemberRecord> replaceDepartments(String memberId, MemberDepartmentInput input) {
        ActiveIdentity actor = current.requireDomain(AuthorizationDomain.TENANT);
        List<MemberDepartmentPlan.Department> departments = new ArrayList<>();
        for (MemberDepartmentBinding binding : input.departments()) {
            departments.add(new MemberDepartmentPlan.Department(binding.id(), binding.primary()));
        }
        lifecycle.replaceDepartments(actor.context(), memberId,
                new MemberDepartmentPlan(input.expectedVersion(), departments), ids.nextId(), guard);
        return queries.get(AuthorizationDomain.TENANT, memberId);
    }
}
