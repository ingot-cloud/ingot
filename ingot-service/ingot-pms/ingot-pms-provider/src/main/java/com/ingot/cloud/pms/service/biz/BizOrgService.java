package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.app.AppEnabledDTO;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;

/**
 * <p>Description  : BizOrgService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 10:56 AM.</p>
 */
public interface BizOrgService {

    /**
     * 条件分页查询
     *
     * @param params 条件
     * @return 返回分页数据
     */
    IPage<SysTenant> conditionPage(Page<SysTenant> page, SysTenant params);

    /**
     * 获取组织权限树
     *
     * @param tenantID 租户权限
     * @return {@link PermissionTreeNodeVO}
     */
    List<PermissionTreeNodeVO> getTenantPermissionTree(long tenantID);

    /**
     * 搜索
     *
     * @param filter 过滤条件
     * @return {@link SysTenant}
     */
    List<SysTenant> search(SysTenant filter);

    /**
     * 获取详情
     *
     * @param id 组织ID
     * @return {@link SysTenant}
     */
    SysTenant getDetails(long id);

    /**
     * 获取组织应用
     *
     * @param tenantId 组织ID
     * @return {@link MetaApp}
     */
    List<MetaApp> getOrgApps(long tenantId);

    /**
     * 更新组织应用状态
     *
     * @param params {@link AppEnabledDTO}
     */
    void updateOrgAppStatus(AppEnabledDTO params);

    /**
     * 创建组织
     *
     * @param params {@link CreateOrgDTO}
     */
    void createOrg(CreateOrgDTO params);

    /**
     * 更新组织基本信息
     *
     * @param params {@link SysTenant}
     */
    void updateBase(SysTenant params);

    /**
     * 删除组织
     *
     * @param id 组织ID
     */
    void removeOrg(long id);
}
