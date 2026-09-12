package com.ingot.cloud.test.service.domain.impl;

import com.ingot.cloud.test.mapper.TDemoAnnouncementMapper;
import com.ingot.cloud.test.model.domain.TDemoAnnouncement;
import com.ingot.cloud.test.service.domain.TDemoAnnouncementService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>{@link TDemoAnnouncementService} 默认实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class TDemoAnnouncementServiceImpl extends BaseServiceImpl<TDemoAnnouncementMapper, TDemoAnnouncement>
        implements TDemoAnnouncementService {
}
