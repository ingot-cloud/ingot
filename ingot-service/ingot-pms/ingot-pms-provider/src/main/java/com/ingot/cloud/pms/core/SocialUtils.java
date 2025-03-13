package com.ingot.cloud.pms.core;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.core.constants.SocialConstants;
import com.ingot.framework.core.model.enums.SocialTypeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : SocialUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/18.</p>
 * <p>Time         : 13:52.</p>
 */
@Slf4j
public class SocialUtils {

    /**
     * 获取微信小程序OpenId
     *
     * @param service {@link SysSocialDetailsService}
     * @param code    小程序登录码
     * @return OpenId
     */
    public static String getMiniProgramOpenId(SysSocialDetailsService service, SocialTypeEnum type, String code) {
        SysSocialDetails socialDetails = service.getOne(
                Wrappers.<SysSocialDetails>lambdaQuery()
                        .eq(SysSocialDetails::getType, type));
        if (socialDetails == null) {
            log.debug("未设置微信小程序appId，appSecret等信息");
            return null;
        }

        String url = String.format(SocialConstants.MINI_APP_AUTHORIZATION_CODE_URL,
                socialDetails.getAppId(), socialDetails.getAppSecret(), code);
        String result = HttpUtil.get(url);
        log.debug("微信小程序响应报文:{}", result);

        Object obj = JSONUtil.parseObj(result).get("openid");
        return obj.toString();
    }
}
