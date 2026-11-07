package com.ingot.cloud.iam.infrastructure.persistence.store;

import com.ingot.cloud.iam.infrastructure.persistence.entity.AccountSocialBindingEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AccountSocialBindingMapper;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-01-19
 */
@Service
public class MybatisAccountSocialBindingStore extends BaseServiceImpl<AccountSocialBindingMapper, AccountSocialBindingEntity> implements AccountSocialBindingStore {

}
