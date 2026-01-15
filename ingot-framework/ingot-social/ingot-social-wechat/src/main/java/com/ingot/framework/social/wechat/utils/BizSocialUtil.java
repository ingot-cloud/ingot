package com.ingot.framework.social.wechat.utils;

import java.util.List;
import java.util.function.Supplier;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.api.rpc.RemotePmsSocialDetailsService;
import com.ingot.framework.commons.constants.SocialConstants;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.model.support.R;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : BizSocialUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 12:31.</p>
 */
@Slf4j
public class BizSocialUtil {
    /**
     * 获取微信小程序OpenId
     *
     * @param supplier {@link Supplier}
     * @param code     小程序登录码
     * @return OpenId
     */
    public static String getMiniProgramOpenId(Supplier<SysSocialDetails> supplier, String code) {
        SysSocialDetails socialDetails = supplier.get();
        if (socialDetails == null) {
            log.debug("未设置微信小程序信息");
            return null;
        }
        String url = String.format(SocialConstants.MINI_APP_AUTHORIZATION_CODE_URL,
                socialDetails.getAppId(), socialDetails.getAppSecret(), code);
        String result = HttpUtil.get(url);
        log.debug("微信小程序响应报文:{}", result);

        Object obj = JSONUtil.parseObj(result).get("openid");
        return obj.toString();
    }

    /**
     * 获取微信小程序OpenId
     *
     * @param service {@link RemotePmsSocialDetailsService}
     * @param code    小程序登录码
     * @return OpenId
     */
    public static String getMiniProgramOpenId(RemotePmsSocialDetailsService service, SocialTypeEnum type, String appId, String code) {
        R<List<SysSocialDetails>> response = service.getSocialDetailsByType(type.getValue());
        if (!response.isSuccess()) {
            log.debug("获取微信小程序信息失败");
            return null;
        }
        List<SysSocialDetails> list = response.getData();
        if (CollUtil.isEmpty(list)) {
            log.debug("未设置微信小程序信息");
            return null;
        }

        return getMiniProgramOpenId(() -> {
            if (StrUtil.isEmpty(appId)) {
                return list.get(0);
            }
            return list.stream().filter(s -> s.getAppId().equals(appId))
                    .findFirst().orElse(null);
        }, code);
    }
}
