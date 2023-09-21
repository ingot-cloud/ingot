package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;

/**
 * <p>Description  : BizOrgService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 10:56 AM.</p>
 */
public interface BizOrgService {

    /**
     * 创建组织
     *
     * @param params {@link CreateOrgDTO}
     */
    void createOrg(CreateOrgDTO params);

    /**
     * 删除组织
     *
     * @param id 组织ID
     */
    void removeOrg(long id);
}
