package com.ingot.cloud.test.service.domain.impl;

import com.ingot.cloud.test.mapper.TClassStudentMapper;
import com.ingot.cloud.test.model.domain.TClassStudent;
import com.ingot.cloud.test.service.domain.TClassStudentService;
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
public class TClassStudentServiceImpl extends BaseServiceImpl<TClassStudentMapper, TClassStudent> implements TClassStudentService {

}
