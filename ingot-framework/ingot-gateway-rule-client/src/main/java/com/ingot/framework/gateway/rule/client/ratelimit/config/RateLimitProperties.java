package com.ingot.framework.gateway.rule.client.ratelimit.config;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.gateway.rule.client.ratelimit.model.EndpointGroup;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitRule;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流域配置：模式开关 + local 模式的内联规则与分组。
 *
 * <p>配置前缀：{@code ingot.security.ratelimit}。</p>
 *
 * <h3>典型配置示例 — local 模式（本机调试 / 单实例）</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     ratelimit:
 *       enabled: true                    # 限流域总开关；为 false 时整个 Sentinel 编译链路不装配
 *       policy:
 *         mode: local                    # local：规则来自下面的 groups / rules；remote：调 ingot-service-security Inner Feign
 *         groups:                        # API 路径分组，便于多条规则复用同一组 path
 *           - code: public-anon          # 分组编码（唯一），规则通过 group-code 引用
 *             name: 匿名公开接口
 *             enabled: true
 *             pattern-list:
 *               - path: /anonymous/**    # Ant 风格；以 /** 结尾走前缀匹配，其他走精确或正则
 *                 method: ANY            # 当前 Sentinel 编译忽略 method，字段保留供将来扩展
 *         rules:
 *           - code: anon-ip              # 规则编码（唯一），同时作为 Sentinel apiName / resource
 *             group-code: public-anon    # 引用分组；与 pattern-list 二选一，留空则用下方内联
 *             dimension: IP              # IP / DEVICE / USER（local 写枚举全名；DB 用短码 IP/DV/UI）
 *             qps: 10                    # 平均速率
 *             burst: 20                  # 突发容量
 *             interval-sec: 1            # 统计窗口（秒）
 *             control-behavior: F        # F=快速失败，Q=排队等待
 *             enabled: true
 *             priority: 0                # 仅用于编译时排序，不影响运行期行为
 *           - code: anon-inline          # 不使用分组，直接内联路径
 *             pattern-list:
 *               - path: /captcha/code
 *                 method: GET
 *             dimension: DEVICE          # 按 In-Ca-Sig 设备指纹限流
 *             qps: 2
 *             burst: 2
 *             interval-sec: 1
 *             enabled: true
 * }</pre>
 *
 * <h3>典型配置示例 — remote 模式（生产 / 多节点）</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     policy:
 *       client:
 *         enabled: true
 *         invalidation-enabled: true    # 必开，否则跨节点改规则后无法热更新
 *     ratelimit:
 *       enabled: true
 *       policy:
 *         mode: remote                  # 规则统一在 Platform 页面维护，下沉到本地 L1 编译缓存
 *         # groups / rules 在 remote 模式下被忽略
 * }</pre>
 *
 * <h3>设计说明</h3>
 * <ul>
 *     <li>未在 {@link Policy#getGroups()} 或 {@link Policy#getRules()} 中显式声明的路径
 *         <b>默认不限流</b>，遵循"白名单式限流"原则。</li>
 *     <li>{@link RateLimitRule#isEnabled()} 为 false 时规则不写入 Sentinel。</li>
 * </ul>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.ratelimit")
public class RateLimitProperties {

    /**
     * 限流域总开关。
     * <p>{@code false}（默认）时：</p>
     * <ul>
     *     <li>{@code RateLimitRuleService} 不装配</li>
     *     <li>{@code SentinelGatewayConfiguration} 因依赖 {@code RateLimitRuleService} 也不会执行 reload</li>
     *     <li>仍然兼容现有 Nacos 直接下发 Sentinel 规则的路径，本 SDK 静默不接管</li>
     * </ul>
     */
    private boolean enabled = false;

    /** 限流策略加载配置：模式 + local 模式下的分组与规则列表。 */
    private Policy policy = new Policy();

    /**
     * 限流策略配置：模式 + local 模式规则。
     */
    @Getter
    @Setter
    public static class Policy {
        /**
         * 加载模式：
         * <ul>
         *     <li>{@link Mode#LOCAL}（默认）— 规则来自本类的 {@link #rules} / {@link #groups}，
         *         适合本机调试、单实例或规则极简的场景。</li>
         *     <li>{@link Mode#REMOTE} — 启动期 + 每次失效后通过
         *         {@code RemoteSnapshotFetcher} 调 ingot-service-security 的
         *         {@code GET /inner/security/policy/snapshot} 拉取，由
         *         Platform 页面维护。</li>
         * </ul>
         */
        private Mode mode = Mode.LOCAL;

        /**
         * local 模式下的限流规则列表；remote 模式下被忽略。
         */
        private List<RateLimitRule> rules = new ArrayList<>();

        /**
         * local 模式下的 API 路径分组列表；可以被 {@link RateLimitRule#getGroupCode()} 引用，
         * 也可以为空（规则全部使用内联 {@code patternList}）。remote 模式下被忽略。
         */
        private List<EndpointGroup> groups = new ArrayList<>();
    }

    /**
     * 限流规则加载模式。
     */
    public enum Mode {
        /**
         * 从本机 yaml {@link Policy#getRules()} / {@link Policy#getGroups()} 加载。
         * 适合本机调试、单实例或规则极简的场景；修改 yaml 后需重启或手动 evict。
         */
        LOCAL,
        /**
         * 从 ingot-service-security 远端快照加载（{@code GET /inner/security/policy/snapshot}）。
         * 适合生产 / 多节点；配合 {@code invalidation-enabled=true} 实现 Platform 改规则后热更新。
         */
        REMOTE
    }
}
