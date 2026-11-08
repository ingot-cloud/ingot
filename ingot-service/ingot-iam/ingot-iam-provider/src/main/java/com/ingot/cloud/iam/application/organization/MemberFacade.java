package com.ingot.cloud.iam.application.organization;

import java.util.List;

import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.application.organization.model.PlatformMemberContext;
import com.ingot.cloud.iam.application.group.model.GroupRecord;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.cloud.iam.domain.organization.MemberCreateInput;
import com.ingot.cloud.iam.application.organization.model.PlatformMemberEditInput;
import com.ingot.cloud.iam.application.organization.model.PlatformMemberEditPreview;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.organization.model.MemberProfileInput;
import com.ingot.cloud.iam.application.organization.model.MemberRecord;
import com.ingot.cloud.iam.application.organization.model.MemberRoleReplaceInput;
import com.ingot.cloud.iam.application.organization.model.MemberRoleView;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * <p>聚合成员读取与变更用例，事务和业务规则由各用例服务维护。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberFacade {
    private final MemberQueryService queries;
    private final MemberProfileCommandService commands;


    /**
     * 读取整份平台成员读策略的列可见性及新成员创建字段上下文，不依赖分页样本。
     * @return 仅用于展示的上下文；提交与逐行展示仍使用真实目标重新校验
     */
    public PlatformMemberContext platformContext() {
        return queries.platformContext();
    }

    /** 返回成员资源的全局操作能力，列表空页也有相同结果。 */
    public com.ingot.framework.commons.model.iam.ResourceFieldContext tenantContext() {
        return queries.tenantContext();
    }

    /**
     * 分页列出当前域成员。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        return queries.list(domain, page, pageSize);
    }

    /**
     * 分页列出平台成员。显示名按包含匹配，资格按稳定字面量精确匹配。
     * @param page 页码
     * @param pageSize 页大小
     * @param name 显示名包含匹配，可空
     * @param status 成员资格，可空；仅接受 ACTIVE、SUSPENDED、REMOVED
     * @param ids 逗号分隔成员 ID，可空；用于已选回显
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listPlatform(int page, int pageSize, String name, String status,
            String ids) {
        return queries.listPlatform(page, pageSize, name, status, ids);
    }

    /**
     * 分页列出当前域成员；原值筛选仅在字段完整可见时允许。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize,
            String phone, String email) {
        return queries.list(domain, page, pageSize, phone, email);
    }

    /**
     * 分页列出当前域成员；原值筛选仅在字段完整可见时允许。
     * 租户列表可按直属部门与资格筛选；不传 status 时排除已移出。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param departmentId 直属任职部门，可空
     * @param status 成员资格，可空；仅接受 ACTIVE、SUSPENDED、REMOVED
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize,
            String phone, String email, String departmentId, String status) {
        return queries.list(domain, page, pageSize, phone, email, departmentId, status);
    }

    /**
     * 已通过 ACTION 校验后按范围与字段策略列出成员，供导出下载重验复用。
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action, int page,
            int pageSize, String phone, String email) {
        return queries.listProjected(actor, action, page, pageSize, phone, email);
    }

    /**
     * 已通过 ACTION 校验后按范围、显示名与资格列出成员。
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param name 平台显示名包含匹配，可空；租户列表忽略
     * @param status 成员资格，可空；不传时租户与平台均排除已移出
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action, int page,
            int pageSize, String phone, String email, String name, MemberStatus status) {
        return queries.listProjected(actor, action, page, pageSize, phone, email, name, status);
    }

    /**
     * 已通过 ACTION 校验后按范围、显示名、资格与成员 ID 列出成员。
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param name 平台显示名包含匹配，可空；租户列表忽略
     * @param status 成员资格，可空；不传时租户与平台均排除已移出
     * @param ids 平台成员 ID，空表示不按 ID 限制
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action, int page,
            int pageSize, String phone, String email, String name, MemberStatus status, List<Long> ids) {
        return queries.listProjected(actor, action, page, pageSize, phone, email, name, status, ids);
    }

    /**
     * 已通过 ACTION 校验后按范围、显示名、资格、成员 ID 与直属部门列出成员。
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param name 平台显示名包含匹配，可空；租户列表忽略
     * @param status 成员资格，可空；不传时租户与平台均排除已移出
     * @param ids 平台成员 ID，空表示不按 ID 限制
     * @param departmentId 直属任职部门，可空；平台列表忽略
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action, int page,
            int pageSize, String phone, String email, String name, MemberStatus status, List<Long> ids,
            String departmentId) {
        return queries.listProjected(actor, action, page, pageSize, phone, email, name, status, ids, departmentId);
    }

    /**
     * 按导出快照 ID 重验范围与字段策略，返回当前仍可见的完整投影。
     * @param actor 当前身份
     * @param action 导出操作
     * @param memberIds 快照成员 ID
     * @return 完整投影结果
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjectedByIds(ActiveIdentity actor, IamAction action,
            List<String> memberIds) {
        return queries.listProjectedByIds(actor, action, memberIds);
    }

    /**
     * 读取当前域成员详情。
     * @param domain 接口管理域
     * @param memberId 成员 ID
     * @return 安全投影
     */
    public ResourceDetail<MemberRecord> get(AuthorizationDomain domain, String memberId) {
        return queries.get(domain, memberId);
    }

    /**
     * 分页列出平台用户组的直接成员，按组成员关系过滤，不接受调用方传入成员 ID。
     * @param groupId 平台组 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param name 显示名包含匹配，可空
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listPlatformGroupMembers(String groupId, int page, int pageSize,
            String name) {
        return queries.listPlatformGroupMembers(groupId, page, pageSize, name);
    }

    /**
     * 分页列出平台成员所在的静态用户组，只返回组名，不展开成员选择。
     * @param memberId 平台成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 组页
     */
    public PageResponse<ResourceDetail<GroupRecord>> listPlatformGroups(String memberId, int page, int pageSize) {
        return queries.listPlatformGroups(memberId, page, pageSize);
    }

    /**
     * 分页列出租户成员所在用户组，含直接加入和部门来源，只返回组名。
     *
     * @param memberId 租户成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 组页
     */
    public PageResponse<ResourceDetail<GroupRecord>> listTenantGroups(String memberId, int page, int pageSize) {
        return queries.listTenantGroups(memberId, page, pageSize);
    }

    /**
     * 列出平台成员的简单直接角色。
     * @param memberId 平台成员 ID
     * @return 按名称排序的直接角色
     */
    public List<MemberRoleView> listDirectRoles(String memberId) {
        return queries.listDirectRoles(memberId);
    }

    /**
     * 替换平台成员的简单直接角色。
     * @param memberId 平台成员 ID
     * @param input 目标角色 ID
     * @return 替换后的直接角色
     */
    public List<MemberRoleView> replaceDirectRoles(String memberId, MemberRoleReplaceInput input) {
        return commands.replaceDirectRoles(memberId, input);
    }

    /**
     * 把已有账号关联为当前域成员。
     * @param domain 接口管理域
     * @param input 账号与任职
     * @return 新成员 ID
     */
    public CreatedResource create(AuthorizationDomain domain, MemberCreateInput input) {
        return commands.create(domain, input);
    }

    /**
     * 更新当前域成员资料，不修改全局账号或成员资格。手机号和邮箱写入当前域成员的联系字段，空引用表示不修改，空白表示清空。不可编辑字段仍拒绝写入。
     * @param domain 接口管理域
     * @param memberId 成员 ID
     * @param input 资料
     * @return 更新后投影
     */
    public ResourceDetail<MemberRecord> patch(AuthorizationDomain domain, String memberId, MemberProfileInput input) {
        return commands.patch(domain, memberId, input);
    }
    /**
     * 只读预览成员资料及角色差量，预览不替代最终事务内重验。
     * @param memberId 成员
     * @param input 平台编辑草稿
     * @return 变化摘要与阻断问题
     */
    public Preview<PlatformMemberEditPreview>
            previewPlatformEdit(String memberId, PlatformMemberEditInput input) {
        return commands.previewPlatformEdit(memberId, input);
    }

    /**
     * 同一事务保存资料和角色差量，失败整批回滚。
     * @param memberId 成员
     * @param input 平台编辑草稿
     * @return 更新后的安全投影
     */
    public ResourceDetail<MemberRecord> patchPlatform(String memberId,
            PlatformMemberEditInput input) {
        return commands.patchPlatform(memberId, input);
    }
}
