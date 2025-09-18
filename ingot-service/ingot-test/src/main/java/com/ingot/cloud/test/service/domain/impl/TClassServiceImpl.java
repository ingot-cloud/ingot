package com.ingot.cloud.test.service.domain.impl;

import com.ingot.cloud.test.mapper.TClassMapper;
import com.ingot.cloud.test.model.domain.TClass;
import com.ingot.cloud.test.service.domain.TClassService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-03-31
 */
@Service
public class TClassServiceImpl extends BaseServiceImpl<TClassMapper, TClass> implements TClassService {

}
