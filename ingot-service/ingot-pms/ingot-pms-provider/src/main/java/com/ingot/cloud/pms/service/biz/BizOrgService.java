package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;

/**
 * <p>Description  : BizOrgService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 10:56 AM.</p>
 */
public interface BizOrgService {

    /**
     * 获取组织权限树
     *
     * @param tenantID 租户权限
     * @return {@link AuthorityTreeNodeVO}
     */
    List<AuthorityTreeNodeVO> getTenantAuthorityTree(long tenantID);

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
