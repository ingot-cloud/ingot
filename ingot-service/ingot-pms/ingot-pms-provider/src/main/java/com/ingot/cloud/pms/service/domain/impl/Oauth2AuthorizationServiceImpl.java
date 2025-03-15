package com.ingot.cloud.pms.service.domain.impl;

import com.ingot.cloud.pms.api.model.domain.Oauth2Authorization;
import com.ingot.cloud.pms.mapper.Oauth2AuthorizationMapper;
import com.ingot.cloud.pms.service.domain.Oauth2AuthorizationService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2021-09-29
 */
@Service
public class Oauth2AuthorizationServiceImpl extends BaseServiceImpl<Oauth2AuthorizationMapper, Oauth2Authorization> implements Oauth2AuthorizationService {

}
