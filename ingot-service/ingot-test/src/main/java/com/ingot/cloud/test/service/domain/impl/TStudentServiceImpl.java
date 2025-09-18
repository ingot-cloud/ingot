package com.ingot.cloud.test.service.domain.impl;

import com.ingot.cloud.test.mapper.TStudentMapper;
import com.ingot.cloud.test.model.domain.TStudent;
import com.ingot.cloud.test.service.domain.TStudentService;
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
public class TStudentServiceImpl extends BaseServiceImpl<TStudentMapper, TStudent> implements TStudentService {

}
