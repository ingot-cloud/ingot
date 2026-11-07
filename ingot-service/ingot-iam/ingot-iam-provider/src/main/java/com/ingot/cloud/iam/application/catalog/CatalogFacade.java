package com.ingot.cloud.iam.application.catalog;

import java.util.List;
import com.ingot.cloud.iam.application.catalog.model.ActionCatalogView;
import com.ingot.cloud.iam.application.catalog.model.GrantCatalogResource;
import com.ingot.cloud.iam.application.catalog.model.ActionDraft;
import com.ingot.cloud.iam.application.catalog.model.ApplicationBundleDraft;
import com.ingot.cloud.iam.application.catalog.model.ActionLookupInput;
import com.ingot.cloud.iam.application.catalog.model.ActionLookupRecord;
import com.ingot.cloud.iam.application.catalog.model.ActionRecord;
import com.ingot.cloud.iam.application.catalog.model.ActionUpdateInput;
import com.ingot.cloud.iam.application.catalog.model.ApplicationDraft;
import com.ingot.cloud.iam.application.catalog.model.ApplicationPurgeInput;
import com.ingot.cloud.iam.application.catalog.model.ApplicationRecord;
import com.ingot.cloud.iam.domain.catalog.ApplicationSummary;
import com.ingot.cloud.iam.application.catalog.model.ApplicationUpdateInput;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.cloud.iam.application.shared.model.ConfigurationStatusInput;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.application.catalog.model.MenuActionRecord;
import com.ingot.cloud.iam.domain.catalog.MenuDraft;
import com.ingot.cloud.iam.application.catalog.model.MenuRecord;
import com.ingot.cloud.iam.application.catalog.model.MenuTreeNode;
import com.ingot.cloud.iam.application.catalog.model.MenuUpdateInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.PlanDraft;
import com.ingot.cloud.iam.application.catalog.model.PlanRecord;
import com.ingot.cloud.iam.application.catalog.model.PlanSummary;
import com.ingot.cloud.iam.application.catalog.model.PlanUpdateInput;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.catalog.model.ResourceDraft;
import com.ingot.cloud.iam.application.catalog.model.ResourceRecord;
import com.ingot.cloud.iam.application.catalog.model.ResourceUpdateInput;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>聚合catalog用例的内部调用入口，规则和事务分别由职责明确的应用服务执行。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CatalogFacade {
    private final ActionCatalogService actionCatalogService;
    private final ApplicationBundleService applicationBundleService;
    private final ApplicationCatalogService applicationCatalogService;
    private final MenuCatalogService menuCatalogService;
    private final PlanCatalogService planCatalogService;
    private final ResourceCatalogService resourceCatalogService;

    /**
     * 分页列出应用目录。
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param domain {@link AuthorizationDomain} 稳定字面量，必填
     * @param name 应用名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @param baseline 是否组织默认开通，空表示不限制
     * @return 应用详情页
     */
    public PageResponse<ResourceDetail<ApplicationRecord>> listApplications(int page, int pageSize, String domain,
            String name, String status, Boolean baseline) {
        return applicationCatalogService.listApplications(page, pageSize, domain, name, status, baseline);
    }

    /**
     * 分页列出开通选择器所需的应用摘要。
     * @param page 从 1 开始的页码
     * @param pageSize 每页条数
     * @param domain 管理域，必填 PLATFORM 或 TENANT
     * @param name 应用名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @param baseline 是否组织默认开通，空表示不限制
     * @return 应用摘要页
     */
    public PageResponse<ResourceDetail<ApplicationSummary>> listApplicationSummaries(int page, int pageSize,
            String domain, String name, String status, Boolean baseline) {
        return applicationCatalogService.listApplicationSummaries(page, pageSize, domain, name, status, baseline);
    }

    /**
     * 读取单个应用。
     * @param id 应用 ID
     * @return 应用详情
     */
    public ResourceDetail<ApplicationRecord> getApplication(String id) {
        return applicationCatalogService.getApplication(id);
    }

    /**
     * 创建应用目录项，不自动为任何租户开通。
     * @param input 创建草稿
     * @return 新应用 ID 与版本
     */
    public CreatedResource createApplication(ApplicationDraft input) {
        return applicationCatalogService.createApplication(input);
    }

    /**
     * 一次创建应用及其资源、操作与菜单；任一步失败整单回滚。
     * @param input 应用、资源与菜单草稿
     * @return 新应用 ID 与版本
     */
    public CreatedResource createApplicationBundle(ApplicationBundleDraft input) {
        return applicationBundleService.createApplicationBundle(input);
    }

    /**
     * 更新展示信息与基础开通标记，不能改写命名空间或管理域。
     * @param id 应用 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ApplicationRecord> updateApplication(String id, ApplicationUpdateInput input) {
        return applicationCatalogService.updateApplication(id, input);
    }

    /**
     * 全局启停应用，停用后该应用全部操作失败关闭。
     * @param id 应用 ID
     * @param input 目标状态与版本
     * @return 提交后版本
     */
    public CreatedResource changeApplicationStatus(String id, ConfigurationStatusInput input) {
        return applicationCatalogService.changeApplicationStatus(id, input);
    }

    /**
     * 删除未被资源、菜单、开通或套餐引用的应用；被拒时说明具体引用。
     * @param id 应用 ID
     * @return 删除前版本
     */
    public CreatedResource deleteApplication(String id) {
        return applicationCatalogService.deleteApplication(id);
    }

    /**
     * 确认当前账号后，同一事务清除该应用全部关联并删除应用行。
     * @param id 应用 ID
     * @param input 版本与内嵌确认
     * @return 删除前版本
     */
    public CreatedResource purgeApplication(String id, ApplicationPurgeInput input) {
        return applicationCatalogService.purgeApplication(id, input);
    }

    /**
     * 列出应用内资源。
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param name 资源名称包含匹配，空白表示不限制
     * @param code 资源编码包含匹配，空白表示不限制
     * @return 资源页
     */
    public PageResponse<ResourceDetail<ResourceRecord>> listResources(String applicationId, int page, int pageSize,
            String name, String code) {
        return resourceCatalogService.listResources(applicationId, page, pageSize, name, code);
    }

    /** 校验目录读取权与归属后返回服务清单；未绑定资源返回空清单。 */
    public com.ingot.framework.commons.model.iam.extension.FieldBindingManifest resourceBindings(String applicationId, String resourceId) {
        return resourceCatalogService.resourceBindings(applicationId, resourceId);
    }

    /**
     * 在指定应用下创建资源。
     * @param applicationId 应用 ID
     * @param input 资源草稿
     * @return 新资源 ID
     */
    public CreatedResource createResource(String applicationId, ResourceDraft input) {
        return resourceCatalogService.createResource(applicationId, input);
    }

    /**
     * 更新资源名称与能力，不能改写编码或所属应用。
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ResourceRecord> updateResource(String applicationId, String resourceId,
            ResourceUpdateInput input) {
        return resourceCatalogService.updateResource(applicationId, resourceId, input);
    }

    /**
     * 删除未被操作引用的资源。
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 删除前版本
     */
    public CreatedResource deleteResource(String applicationId, String resourceId) {
        return resourceCatalogService.deleteResource(applicationId, resourceId);
    }

    /**
     * 列出应用内精确操作。
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param resourceId 所属资源，空白表示不限制
     * @param name 操作名称包含匹配，空白表示不限制
     * @param ids 逗号分隔操作 ID，空白表示不限制
     * @return 操作页
     */
    public PageResponse<ResourceDetail<ActionRecord>> listActions(String applicationId, int page, int pageSize,
            String resourceId, String name, String ids) {
        return actionCatalogService.listActions(applicationId, page, pageSize, resourceId, name, ids);
    }

    /**
     * 一次返回应用内资源及操作树，供菜单选择操作。
     * @param applicationId 应用 ID
     * @return 操作目录
     */
    public ActionCatalogView getActionCatalog(String applicationId) {
        return actionCatalogService.getActionCatalog(applicationId);
    }

    /**
     * 一次返回应用内全部启用资源及其启用操作，供角色权限选择器组树。
     *
     * @param applicationId 应用 ID
     * @return 授权选择目录
     */
    public List<GrantCatalogResource> listGrantCatalog(String applicationId) {
        return actionCatalogService.listGrantCatalog(applicationId);
    }

    /**
     * 一次返回指定资源下的全部操作，供权限树展开。
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 操作列表
     */
    public List<ActionRecord> listResourceActions(String applicationId, String resourceId) {
        return actionCatalogService.listResourceActions(applicationId, resourceId);
    }

    /**
     * 一次返回菜单已绑定操作及资源名称，供详情回显。
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @return 已关联操作
     */
    public List<MenuActionRecord> listMenuActions(String applicationId, String menuId) {
        return menuCatalogService.listMenuActions(applicationId, menuId);
    }

    /**
     * 按操作 ID 批量解析名称、应用与资源，供权限回显。
     * @param input 操作 ID
     * @return 解析结果；未命中的 ID 省略
     */
    public List<ActionLookupRecord> lookupActions(ActionLookupInput input) {
        return actionCatalogService.lookupActions(input);
    }

    /**
     * 按操作 ID 解析目录内容，不校验 ACTION；调用方须已完成自身读授权。
     * @param ids 操作 ID
     * @return 解析结果；未命中的 ID 省略
     */
    public List<ActionLookupRecord> resolveActions(List<String> ids) {
        return actionCatalogService.resolveActions(ids);
    }

    /**
     * 创建绑定资源的精确操作，操作码不得含通配符。
     * @param applicationId 应用 ID
     * @param input 操作草稿
     * @return 新操作 ID
     */
    public CreatedResource createAction(String applicationId, ActionDraft input) {
        return actionCatalogService.createAction(applicationId, input);
    }

    /**
     * 更新操作名称，不能改写操作码或所属资源。
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ActionRecord> updateAction(String applicationId, String actionId, ActionUpdateInput input) {
        return actionCatalogService.updateAction(applicationId, actionId, input);
    }

    /**
     * 全局启停操作。
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    public CreatedResource changeActionStatus(String applicationId, String actionId, ConfigurationStatusInput input) {
        return actionCatalogService.changeActionStatus(applicationId, actionId, input);
    }

    /**
     * 删除未被菜单或角色引用的操作。
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @return 删除前版本
     */
    public CreatedResource deleteAction(String applicationId, String actionId) {
        return actionCatalogService.deleteAction(applicationId, actionId);
    }

    /**
     * 列出应用菜单。
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 菜单页
     */
    public PageResponse<ResourceDetail<MenuRecord>> listMenus(String applicationId, int page, int pageSize) {
        return menuCatalogService.listMenus(applicationId, page, pageSize);
    }

    /**
     * 返回应用菜单树，不带分页。
     * @param applicationId 应用 ID
     * @return 根节点
     */
    public List<MenuTreeNode> listMenuTree(String applicationId) {
        return menuCatalogService.listMenuTree(applicationId);
    }

    /**
     * 创建菜单并绑定本应用精确操作。
     * @param applicationId 应用 ID
     * @param input 菜单草稿
     * @return 新菜单 ID
     */
    public CreatedResource createMenu(String applicationId, MenuDraft input) {
        return menuCatalogService.createMenu(applicationId, input);
    }

    /**
     * 整体更新菜单并重验本应用操作引用。
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<MenuRecord> updateMenu(String applicationId, String menuId, MenuUpdateInput input) {
        return menuCatalogService.updateMenu(applicationId, menuId, input);
    }

    /**
     * 删除没有子菜单的菜单。
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @return 删除前版本
     */
    public CreatedResource deleteMenu(String applicationId, String menuId) {
        return menuCatalogService.deleteMenu(applicationId, menuId);
    }

    /**
     * 分页列出套餐。
     * @param page 页码
     * @param pageSize 页大小
     * @param name 套餐名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @return 套餐页
     */
    public PageResponse<ResourceDetail<PlanRecord>> listPlans(int page, int pageSize, String name, String status) {
        return planCatalogService.listPlans(page, pageSize, name, status);
    }

    /**
     * 分页列出套餐选择器所需的最小标识，不附带应用清单。
     * @param page 页码
     * @param pageSize 页大小
     * @param name 套餐名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @return 套餐摘要页
     */
    public PageResponse<ResourceDetail<PlanSummary>> listPlanSummaries(int page, int pageSize, String name,
            String status) {
        return planCatalogService.listPlanSummaries(page, pageSize, name, status);
    }

    /**
     * 读取套餐详情。
     * @param id 套餐 ID
     * @return 套餐详情
     */
    public ResourceDetail<PlanRecord> getPlan(String id) {
        return planCatalogService.getPlan(id);
    }

    /**
     * 创建套餐。修改套餐不会改变既有租户开通。
     * @param input 套餐草稿
     * @return 新套餐 ID
     */
    public CreatedResource createPlan(PlanDraft input) {
        return planCatalogService.createPlan(input);
    }

    /**
     * 整体替换套餐应用清单，不影响已开通租户。
     * @param id 套餐 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<PlanRecord> updatePlan(String id, PlanUpdateInput input) {
        return planCatalogService.updatePlan(id, input);
    }
}
