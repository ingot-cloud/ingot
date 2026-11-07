package com.ingot.cloud.iam.application.social;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ingot.cloud.iam.infrastructure.persistence.entity.SocialConfigEntity;
import com.ingot.cloud.iam.infrastructure.persistence.store.SocialConfigStore;
import com.ingot.cloud.iam.application.social.model.SocialConfigInput;
import com.ingot.cloud.iam.application.social.model.SocialConfigRecord;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ingot.framework.tenant.TenantContextHolder;

/**
 * <p>编排社交渠道配置维护，保留事务、密钥处理及配置刷新顺序。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SocialConfigService {
    private final IamAccess access;
    private final SocialConfigStore socials;
    private final WxMaService wxMaService;

    /** 按原分页和筛选条件读取配置响应。 */
    public IPage<SocialConfigRecord> list(Page<SocialConfigRecord> page, SocialConfigInput condition) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_SOCIAL_CONFIG_READ);
        IPage<SocialConfigEntity> queryPage = page.convert(ignored -> null);
        return socials.page(queryPage, Wrappers.lambdaQuery(SocialConfigTransfer.toEntity(condition)))
                .convert(SocialConfigTransfer::toRecord);
    }

    /** 按精确操作资格创建配置。 */
    @Transactional(rollbackFor = Exception.class)
    public void create(SocialConfigInput input) {
        SocialConfigEntity params = SocialConfigTransfer.toEntity(input);
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_SOCIAL_CONFIG_CREATE);
        params.setTenantId(TenantContextHolder.get());
        params.setCreatedAt(DateUtil.utc());
        params.setUpdatedAt(DateUtil.utc());
        socials.save(params);
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(params.getAppId());
        config.setSecret(params.getAppSecret());
        wxMaService.addConfig(params.getAppId(), config);
    }

    /** 按路径标识更新配置并保留原副作用顺序。 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SocialConfigInput input) {
        SocialConfigEntity params = SocialConfigTransfer.toEntity(input);
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_SOCIAL_CONFIG_UPDATE);
        params.setId(id);
        params.setUpdatedAt(DateUtil.utc());
        socials.updateById(params);
        if (StringUtils.isNotEmpty(params.getAppSecret())) {
            SocialConfigEntity current = socials.getById(id);
            WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
            config.setAppid(current.getAppId());
            config.setSecret(current.getAppSecret());
            wxMaService.addConfig(current.getAppId(), config);
        }
    }

    /** 按精确操作资格删除配置。 */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_SOCIAL_CONFIG_DELETE);
        SocialConfigEntity current = socials.getById(id);
        if (current != null) {
            wxMaService.removeConfig(current.getAppId());
        }
        socials.removeById(id);
    }
}
