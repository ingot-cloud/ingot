package com.ingot.cloud.test.service.domain.impl;

import com.ingot.cloud.test.mapper.TDemoOrderMapper;
import com.ingot.cloud.test.model.domain.TDemoOrder;
import com.ingot.cloud.test.service.domain.TDemoOrderService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>{@link TDemoOrderService} 默认实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class TDemoOrderServiceImpl extends BaseServiceImpl<TDemoOrderMapper, TDemoOrder> implements TDemoOrderService {
}
