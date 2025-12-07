package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.biz.SocialDetailsService;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : SocialDetailsServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 16:05.</p>
 */
@Service
@RequiredArgsConstructor
public class SocialDetailsServiceImpl implements SocialDetailsService {
    private final SysSocialDetailsService sysSocialDetailsService;

    @Override
    public List<SysSocialDetails> getSocialDetailsByType(SocialTypeEnum type) {
        return CollUtil.emptyIfNull(sysSocialDetailsService.list(Wrappers.<SysSocialDetails>lambdaQuery()
                .in(SysSocialDetails::getType, type)));
    }

    @Override
    public SysSocialDetails getSocialDetailsByAppId(String appId) {
        return sysSocialDetailsService.getOne(Wrappers.<SysSocialDetails>lambdaQuery()
                .eq(SysSocialDetails::getAppId, appId));
    }
}
