package com.ingot.cloud.iam.application.delegation;

import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationInput;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationMine;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationRecord;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationUpdateInput;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.shared.model.ReferenceImpactPreview;
import java.time.*;
import java.util.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>聚合delegation用例的内部调用入口，规则和事务分别由职责明确的应用服务执行。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantDelegationFacade {
    private final TenantDelegationCommandService tenantDelegationCommandService;
    private final TenantDelegationQueryService tenantDelegationQueryService;

    /** 治理范围内分页查询委派。 */
    public PageResponse<ResourceDetail<TenantDelegationRecord>> list(int page,int size,String keyword){
        return tenantDelegationQueryService.list(page, size, keyword);
    }
    /** 读取可见委派，委派入口不授予独立治理资格。 */
    public ResourceDetail<TenantDelegationRecord> get(String id){
        return tenantDelegationQueryService.get(id);
    }
    /** 原子创建完整委派。 */
    public CreatedResource create(TenantDelegationInput input){
        return tenantDelegationCommandService.create(input);
    }
    /** 原子更新限制，所有派生分配下一请求执行当前上限。 */
    public ResourceDetail<TenantDelegationRecord> replace(String id,TenantDelegationUpdateInput input){
        return tenantDelegationCommandService.replace(id, input);
    }
    /** 撤销来源并保留派生记录与审计。 */
    public CreatedResource delete(String id){
        return tenantDelegationCommandService.delete(id);
    }
    /** 校验拟更新委派，受影响细节按当前可见分配范围披露。 */
    public Preview<ReferenceImpactPreview> preview(String id,TenantDelegationUpdateInput input){
        return tenantDelegationQueryService.preview(id, input);
    }
    /** 返回本人有效委派摘要，不依赖委派管理操作。 */
    public List<TenantDelegationMine> mine(){
        return tenantDelegationQueryService.mine();
    }
    /** 独立治理者按实际资源参数查询范围对象。 */
    public AuthorizationCandidatePage scopeCandidates(String resourceId,String key,String keyword,List<String> ids,int page,int size){
        return tenantDelegationQueryService.scopeCandidates(resourceId, key, keyword, ids, page, size);
    }
}
