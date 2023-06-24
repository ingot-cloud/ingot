package com.ingot.cloud.pms.service.domain.impl;

import com.ingot.cloud.pms.api.model.domain.Oauth2AuthorizationConsent;
import com.ingot.cloud.pms.mapper.Oauth2AuthorizationConsentMapper;
import com.ingot.cloud.pms.service.domain.Oauth2AuthorizationConsentService;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
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
public class Oauth2AuthorizationConsentServiceImpl extends BaseServiceImpl<Oauth2AuthorizationConsentMapper, Oauth2AuthorizationConsent> implements Oauth2AuthorizationConsentService {

}
