package com.ingot.cloud.gateway.security;

import com.ingot.framework.gateway.rule.client.challenge.internal.ChallengeTypes;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.vc.common.VCConstants;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.experimental.UtilityClass;

/**
 * <p>组装 HTTP 412 {@code CHALLENGE_REQUIRED} 响应体中的 {@code data}，供客户端动态完成验码并重试原请求。</p>
 *
 * <p>{@link ChallengeFilter} 与 {@link SentinelBlockHandler} 共用本方法。
 * 不返回 PassToken 的 TTL / 剩余次数：那是服务端 Redis 约束，前端按本 payload 走完滑块即可；
 * token 失效后会再次 412。</p>
 *
 * <h3>客户端约定</h3>
 * <p>任意经网关的请求都可能返回本 payload。拦截器按字段名动态拼拉码、验码与重试 Header，
 * 不要写死 {@code /vc/image} 或 {@code In-Vc-Scope}。</p>
 *
 * <pre>{@code
 * {
 *   "code": "CHALLENGE_REQUIRED",
 *   "msg": "Captcha required",
 *   "data": {
 *     "vcType": "image",
 *     "checkPath": "/vc/image/check",
 *     "scope": "login",
 *     "scopeParam": "In-Vc-Scope",
 *     "passTokenParam": "In-Vc-Pass-Token"
 *   }
 * }
 * }</pre>
 *
 * @author jy
 * @since 2026/5/28
 * @see VCConstants#HEADER_SCOPE
 * @see VCConstants#HEADER_PASS_TOKEN
 */
@UtilityClass
public class ChallengeResponses {

    /**
     * 拉码路径中的 VC 类型段，L6 为 {@link ChallengeTypes#VC_IMAGE}。
     */
    public static final String FIELD_VC_TYPE = "vcType";

    /**
     * 验码 HTTP 路径，例如 {@code /vc/image/check}。
     */
    public static final String FIELD_CHECK_PATH = "checkPath";

    /**
     * PassToken 作用域值，须原样作为 {@link #FIELD_SCOPE_PARAM} 所指请求头的值。
     */
    public static final String FIELD_SCOPE = "scope";

    /**
     * 作用域请求头名，值为 {@link VCConstants#HEADER_SCOPE}。
     */
    public static final String FIELD_SCOPE_PARAM = "scopeParam";

    /**
     * PassToken 请求头名，值为 {@link VCConstants#HEADER_PASS_TOKEN}。
     */
    public static final String FIELD_PASS_TOKEN_PARAM = "passTokenParam";

    /**
     * 按命中策略组装 412 {@code data}。不含 ttl / remaining。
     *
     * @param policy 命中的挑战策略，{@code challengeType} 须为 IMAGE/SLIDER
     * @return 有序字段 map
     */
    public static Map<String, Object> buildPayload(ChallengePolicy policy) {
        Map<String, Object> data = new LinkedHashMap<>();
        String vcType = ChallengeTypes.toVcType(policy.getChallengeType());
        data.put(FIELD_VC_TYPE, vcType);
        data.put(FIELD_CHECK_PATH, VCConstants.PATH_PREFIX + "/" + vcType + "/check");
        data.put(FIELD_SCOPE, policy.getScope());
        data.put(FIELD_SCOPE_PARAM, VCConstants.HEADER_SCOPE);
        data.put(FIELD_PASS_TOKEN_PARAM, VCConstants.HEADER_PASS_TOKEN);
        return data;
    }
}
