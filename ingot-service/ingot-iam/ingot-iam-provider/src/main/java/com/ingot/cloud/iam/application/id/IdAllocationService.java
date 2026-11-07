package com.ingot.cloud.iam.application.id;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IdAllocationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.store.IdAllocationStore;
import com.ingot.cloud.iam.application.id.model.IdAllocationInput;
import com.ingot.cloud.iam.application.id.model.IdAllocationRecord;

/**
 * <p>编排发号配置维护，沿用原算法、高水位和精确操作准入。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class IdAllocationService {
    private final IamAccess access;
    private final IdAllocationStore allocations;

    /** 按原分页和筛选条件读取配置响应。 */
    public IPage<IdAllocationRecord> list(Page<IdAllocationRecord> page, IdAllocationInput condition) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ID_ALLOCATION_READ);
        IPage<IdAllocationEntity> queryPage = page.convert(ignored -> null);
        return allocations.page(queryPage, Wrappers.lambdaQuery(IdAllocationTransfer.toEntity(condition)))
                .convert(IdAllocationTransfer::toRecord);
    }

    /** 按精确操作资格创建配置。 */
    public void create(IdAllocationInput input) {
        IdAllocationEntity params = IdAllocationTransfer.toEntity(input);
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ID_ALLOCATION_CREATE);
        params.setUpdateTime(DateUtil.utc());
        allocations.save(params);
    }

    /** 按路径标识更新配置并保留原副作用顺序。 */
    public void update(String id, IdAllocationInput input) {
        IdAllocationEntity params = IdAllocationTransfer.toEntity(input);
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ID_ALLOCATION_UPDATE);
        params.setBizTag(id);
        params.setUpdateTime(DateUtil.utc());
        allocations.updateById(params);
    }

    /** 按精确操作资格删除配置。 */
    public void remove(String id) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ID_ALLOCATION_DELETE);
        allocations.removeById(id);
    }
}
