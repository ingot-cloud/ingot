package com.ingot.framework.gateway.rule.client.challenge.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import lombok.*;

/**
 * 挑战策略定义 POJO，对应表 {@code gateway_challenge_policy}。
 *
 * <p>网关根据 {@link #trigger} 与路径匹配结果决定是否返回 412 挑战响应；
 * 用户完成验证码后签发 {@code PassToken}，其 TTL / 可消费次数由
 * {@link #passTokenTtlSec}、{@link #passTokenRemaining} 控制，
 * {@link #scope} 作为 Redis key 的命名空间段。</p>
 *
 * <p>{@link #groupCode} 与 {@link #patternList} 二选一：优先 {@code groupCode} 关联
 * {@link com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup}；
 * 为空则使用内联 {@code patternList}。</p>
 *
 * <h3>典型配置示例 — local 模式</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     challenge:
 *       enabled: true
 *       policy:
 *         mode: local
 *         groups:
 *           - code: login-flow
 *             name: 登录相关接口
 *             enabled: true
 *             pattern-list:
 *               - path: /auth/token
 *                 method: POST
 *         policies:
 *           - code: login-always
 *             group-code: login-flow
 *             trigger: ALWAYS
 *             challenge-type: SLIDER
 *             scope: login
 *             pass-token-ttl-sec: 300
 *             pass-token-remaining: 3
 *             enabled: true
 *             priority: 0
 *           - code: anon-rate-limit
 *             pattern-list:
 *               - path: /anonymous/**
 *                 method: ANY
 *             trigger: ON_RATE_LIMIT
 *             challenge-type: SLIDER
 *             scope: anon
 *             pass-token-ttl-sec: 120
 *             pass-token-remaining: 1
 *             enabled: true
 *             priority: 10
 * }</pre>
 *
 * <p>remote 模式下策略由 Platform 下发，见
 * {@link com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengePolicy implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（remote 快照可能为空；local yaml 可不填）。
     */
    private Long id;

    /**
     * 策略唯一编码，管理面与快照内引用标识。
     */
    private String code;

    /**
     * 引用的 API 路径分组编码；与 {@link #patternList} 二选一。
     * 非空时由 {@link com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties.Policy#getGroups()}
     * 或 remote 快照中的分组定义解析出实际 path 列表。
     */
    private String groupCode;

    /**
     * 内联网关路径模式列表（Ant 风格 path + HTTP method）；
     * {@link #groupCode} 非空时被忽略。
     */
    private List<EndpointPattern> patternList;

    /**
     * 挑战触发条件。
     * <ul>
     *     <li>{@link ChallengeTrigger#ALWAYS}：匹配路径的请求一律先挑战。</li>
     *     <li>{@link ChallengeTrigger#ON_RATE_LIMIT}：Sentinel 限流命中后再挑战，验码通过可放行。</li>
     * </ul>
     */
    private ChallengeTrigger trigger;

    /**
     * 验证码类型：{@code SLIDER} / {@code IMAGE} / {@code SMS} / {@code EMAIL}。
     * 网关侧映射为 VC 路由类型（如 SLIDER → image），见
     * {@link com.ingot.framework.gateway.rule.client.challenge.internal.ChallengeTypes}。
     */
    private String challengeType;

    /**
     * PassToken 有效期（秒）。
     * 用户完成验证码后签发的通行令牌在 Redis 中的 TTL；
     * 客户端在后续请求携带 {@code _vc_pass_token} 可跳过限流或 ALWAYS 挑战。
     */
    private int passTokenTtlSec;

    /**
     * PassToken 可消费次数。
     * 每次成功消费后递减，归零则删除 Redis key，需重新完成挑战。
     */
    private int passTokenRemaining;

    /**
     * 策略作用域，与 PassToken Redis key 的 scope 段关联
     * （格式：{@code in:gw:pass:{scope}:{token}}）。
     * 不同 scope 的令牌互不通用；缺省时网关使用默认 scope。
     */
    private String scope;

    /**
     * 为 {@code false} 时编译阶段跳过本策略，不参与路径匹配与挑战下发。
     */
    private boolean enabled;

    /**
     * 编译时排序权重，数值越小越先匹配；不影响 PassToken 或验证码运行期行为。
     */
    private int priority;
}
