package com.ingot.cloud.member.common;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberSocialDetails;
import com.ingot.cloud.member.service.domain.MemberSocialDetailsService;
import com.ingot.framework.commons.constants.SocialConstants;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : BizSocialUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/18.</p>
 * <p>Time         : 13:52.</p>
 */
@Slf4j
public class BizSocialUtils {

    /**
     * 获取微信小程序OpenId
     *
     * @param service {@link MemberSocialDetailsService}
     * @param code    小程序登录码
     * @return OpenId
     */
    public static String getMiniProgramOpenId(MemberSocialDetailsService service, SocialTypeEnum type, String code) {
        MemberSocialDetails socialDetails = service.getOne(
                Wrappers.<MemberSocialDetails>lambdaQuery()
                        .eq(MemberSocialDetails::getType, type));
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
