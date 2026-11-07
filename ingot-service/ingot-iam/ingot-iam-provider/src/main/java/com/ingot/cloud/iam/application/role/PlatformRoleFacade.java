package com.ingot.cloud.iam.application.role;

import java.util.List;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.ConfigurationStatusInput;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.application.diagnosis.model.EffectiveRole;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.RoleCreateInput;
import com.ingot.cloud.iam.application.role.model.RoleDefinitionDraft;
import com.ingot.cloud.iam.application.role.model.RoleGrantRecord;
import com.ingot.cloud.iam.application.role.model.RoleGrantList;
import com.ingot.cloud.iam.application.role.model.RolePublishInput;
import com.ingot.cloud.iam.application.role.model.RoleRevision;
import com.ingot.cloud.iam.application.role.model.RoleSummary;
import com.ingot.cloud.iam.application.role.model.RoleUpdateInput;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>聚合role用例的内部调用入口，规则和事务分别由职责明确的应用服务执行。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformRoleFacade {
    private final PlatformRoleCommandService platformRoleCommandService;
    private final PlatformRolePreviewService platformRolePreviewService;
    private final PlatformRoleQueryService platformRoleQueryService;
    private final PlatformRoleRevisionService platformRoleRevisionService;

    /**
     * 列出当前路径可见的角色目录。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param page 页码
     * @param pageSize 页大小
     * @return 角色页，不按名称或状态筛选
     */
    public PageResponse<ResourceDetail<RoleSummary>> list(AuthorizationDomain domain, boolean shared, int page,
            int pageSize) {
        return platformRoleQueryService.list(domain, shared, page, pageSize);
    }

    /**
     * 列出当前路径可见的角色目录，可按名称包含匹配和启停状态筛选。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param page 页码
     * @param pageSize 页大小
     * @param name 角色名称包含匹配，空白表示不限制
     * @param status 启停状态，空白表示不限制；仅接受 ENABLED/DISABLED
     * @return 角色页
     * @throws BizException 状态字面量非法时为 {@link IamReasonCode#INVALID_ARGUMENT}
     */
    public PageResponse<ResourceDetail<RoleSummary>> list(AuthorizationDomain domain, boolean shared, int page,
            int pageSize, String name, String status) {
        return platformRoleQueryService.list(domain, shared, page, pageSize, name, status);
    }

    /**
     * 读取角色元数据。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @return 角色详情
     */
    public ResourceDetail<RoleSummary> get(AuthorizationDomain domain, boolean shared, String id) {
        return platformRoleQueryService.get(domain, shared, id);
    }

    /**
     * 读取角色最新已发布版本的当前绑定权限，带目录展示内容。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @return 同一固定版本的当前绑定权限及摘要；尚无版本时为空列表且省略摘要
     */
    public RoleGrantList listCurrentGrants(AuthorizationDomain domain, boolean shared, String id) {
        return platformRoleQueryService.listCurrentGrants(domain, shared, id);
    }

    /**
     * 读取同角色任一已发布版本的完整合成授权与字段快照，供历史编辑使用。
     * @param domain 管理域
     * @param shared 是否共享入口
     * @param id 角色ID
     * @param revisionId 版本ID
     * @return 不省略字段权限的固定授权
     */
    public List<RoleGrantRecord> listRevisionGrants(AuthorizationDomain domain, boolean shared, String id, String revisionId) {
        return platformRoleQueryService.listRevisionGrants(domain, shared, id, revisionId);
    }

    /**
     * 创建角色并发布首个不可变版本，不能创建系统治理角色。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param input 创建命令
     * @return 新角色 ID
     */
    public CreatedResource create(AuthorizationDomain domain, boolean shared, RoleCreateInput input) {
        return platformRoleCommandService.create(domain, shared, input);
    }

    /**
     * 启停角色，停用约束所有旧版本。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    public CreatedResource changeStatus(AuthorizationDomain domain, boolean shared, String id,
            ConfigurationStatusInput input) {
        return platformRoleCommandService.changeStatus(domain, shared, id, input);
    }

    /**
     * 更新角色名称、说明、分组与启停，编码与已发布版本不变。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 待写入的基本信息
     * @return 提交后版本
     */
    public CreatedResource updateProfile(AuthorizationDomain domain, boolean shared, String id, RoleUpdateInput input) {
        return platformRoleCommandService.updateProfile(domain, shared, id, input);
    }

    /**
     * 删除未被授权引用的非系统角色。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @return 删除前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, boolean shared, String id) {
        return platformRoleCommandService.delete(domain, shared, id);
    }

    /**
     * 列出角色不可变版本。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 版本页
     */
    public PageResponse<ResourceDetail<RoleRevision>> listRevisions(AuthorizationDomain domain, boolean shared,
            String id, int page, int pageSize) {
        return platformRoleRevisionService.listRevisions(domain, shared, id, page, pageSize);
    }

    /** 读取同角色的固定版本，受当前域与对象查看权限约束。 */
    public ResourceDetail<RoleRevision> getRevision(AuthorizationDomain domain, boolean shared, String id, String revisionId) {
        return platformRoleRevisionService.getRevision(domain, shared, id, revisionId);
    }

    /** 修改非系统角色的版本说明；不改变权限定义或清理授权缓存。 */
    public CreatedResource updateRevisionMetadata(AuthorizationDomain domain, boolean shared, String id, String revisionId,
            com.ingot.cloud.iam.application.role.model.RoleRevisionMetadataInput input) {
        return platformRoleRevisionService.updateRevisionMetadata(domain, shared, id, revisionId, input);
    }

    /**
     * 发布新版本，不改写既有授权引用。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 新版本 ID
     */
    public CreatedResource publish(AuthorizationDomain domain, boolean shared, String id, RolePublishInput input) {
        return platformRoleRevisionService.publish(domain, shared, id, input);
    }

    /**
     * 预览待发布定义的合成结果，无写入。
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 合成预览
     */
    public Preview<EffectiveRole> preview(AuthorizationDomain domain, boolean shared, String id,
            RoleDefinitionDraft input) {
        return platformRolePreviewService.preview(domain, shared, id, input);
    }

    /**
     * 按明确所选历史版本的固定共享基础预览，不误用最新基础。
     * @param domain 管理域
     * @param shared 共享入口
     * @param id 角色ID
     * @param input 草稿
     * @param sourceRevisionId 可选历史来源
     * @return 合成预览
     */
    public Preview<EffectiveRole> preview(AuthorizationDomain domain, boolean shared, String id,
            RoleDefinitionDraft input, String sourceRevisionId) {
        return platformRolePreviewService.preview(domain, shared, id, input, sourceRevisionId);
    }

    /**
     * 创建角色前只读校验完整定义，包括资源字段能力，不分配版本 ID。
     * @param input 平台角色创建草稿
     * @return 固化默认值后的预览
     */
    public Preview<RoleDefinitionDraft> previewCreate(RoleCreateInput input) {
        return platformRolePreviewService.previewCreate(input);
    }

    /**
     * 合成指定版本的有效授权，供求值引擎复用。
     * @param revisionId 版本 ID
     * @return 合成后的操作范围
     */
    public List<ActionGrant> synthesizedGrants(long revisionId) {
        return platformRoleRevisionService.synthesizedGrants(revisionId);
    }

    /**
     * 读取当前入口的角色定义，不装配展示能力；调用方仍须完成操作准入和原有对象范围门禁。
     * @param domain 可信接口域
     * @param shared 是否共享入口
     * @param actor 已准入身份
     * @param id 角色 ID
     * @return 同域同租户角色定义
     */
    public IamRoleDefinitionEntity requireDefinition(AuthorizationDomain domain, boolean shared, ActiveIdentity actor,
            long id) {
        return platformRoleQueryService.requireDefinition(domain, shared, actor, id);
    }
}
