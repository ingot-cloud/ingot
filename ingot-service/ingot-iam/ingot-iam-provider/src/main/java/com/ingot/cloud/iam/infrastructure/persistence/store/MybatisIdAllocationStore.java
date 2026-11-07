package com.ingot.cloud.iam.infrastructure.persistence.store;

import com.ingot.cloud.iam.infrastructure.persistence.entity.IdAllocationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IdAllocationMapper;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-10-24
 */
@Service
public class MybatisIdAllocationStore extends BaseServiceImpl<IdAllocationMapper, IdAllocationEntity> implements IdAllocationStore {

}
