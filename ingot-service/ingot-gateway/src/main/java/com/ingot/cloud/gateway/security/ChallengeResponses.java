package com.ingot.cloud.gateway.security;

import com.ingot.framework.gateway.rule.client.challenge.internal.ChallengeTypes;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.vc.common.VCConstants;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.experimental.UtilityClass;

/**
 * HTTP 412 挑战响应 {@code data} 字段组装工具。
 *
 * <p>{@link ChallengeFilter} 与 {@link SentinelBlockHandler} 在返回
 * {@link GatewaySecurityConstants#CODE_CHALLENGE_REQUIRED} 时调用
 * {@link #buildPayload}，告知客户端如何完成验证码校验并携带 PassToken。</p>
 *
 * <h3>输出字段</h3>
 * <ul>
 *     <li>{@code vcType} — 验证码类型，由 {@code ChallengeTypes.toVcType} 转换</li>
 *     <li>{@code scope} / {@code scopeParam} — PassToken 作用域及查询参数名</li>
 *     <li>{@code passTokenParam} — 客户端回传 token 的查询参数名（{@code _vc_pass_token}）</li>
 *     <li>{@code checkPath} — 验证码校验 API 路径前缀</li>
 *     <li>{@code ttlSec} / {@code remaining} — 签发 PassToken 的有效期与可用次数</li>
 * </ul>
 *
 * <h3>响应示例</h3>
 * <pre>{@code
 * {
 *   "code": "CHALLENGE_REQUIRED",
 *   "msg": "Captcha required",
 *   "data": {
 *     "vcType": "slider",
 *     "scope": "login",
 *     "scopeParam": "_vc_scope",
 *     "passTokenParam": "_vc_pass_token",
 *     "checkPath": "/vc/slider/check",
 *     "ttlSec": 300,
 *     "remaining": 5
 *   }
 * }
 * }</pre>
 *
 * @author jy
 * @since 2026/5/28
 */
@UtilityClass
public class ChallengeResponses {

    public static Map<String, Object> buildPayload(ChallengePolicy policy) {
        Map<String, Object> data = new LinkedHashMap<>();
        String vcType = ChallengeTypes.toVcType(policy.getChallengeType());
        data.put("vcType", vcType);
        data.put("scope", policy.getScope());
        data.put("scopeParam", VCConstants.QUERY_PARAMS_SCOPE);
        data.put("passTokenParam", VCConstants.QUERY_PARAMS_PASS_TOKEN);
        data.put("checkPath", VCConstants.PATH_PREFIX + "/" + vcType + "/check");
        data.put("ttlSec", policy.getPassTokenTtlSec());
        data.put("remaining", policy.getPassTokenRemaining());
        return data;
    }
}
