package com.ingot.framework.security.core.userdetails;

/**
 * {@link InUser#getMeta()} 所用 key 的常量定义。
 * <p>
 * meta 仅用于登录流程的精细化决策，不参与 JWT 序列化；
 * 由 PMS/Member 在 {@code UserDetailsResponse.meta} 中填充，
 * 经 {@code OAuth2UserDetailsService.parse()} 透传到 {@link InUser#getMeta()}，
 * 供 {@code InUserDetailsChecker} / {@code DefaultUserCredentialChecker} 读取使用。
 * </p>
 * <p>
 * <b>类型约定</b>：meta 经 Feign/Jackson 反序列化后会丢失具体类型（如 LocalDateTime 退化为字符串、
 * Integer 可能变 Long），因此填充端应优先使用字符串/基本数值类型，读取端统一通过
 * {@link InUser#getMetaValue(String, Class)} 获取，该方法会做 ISO 字符串 ↔ 时间类型、
 * Number 互转等兼容处理。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface InUserMetaKeys {

    /**
     * 临时锁定到期时间。
     * <p>传输形态为 ISO-8601 字符串（如 {@code 2026-02-13T12:34:56}），
     * 读取时通过 {@link InUser#getMetaValue(String, Class)} 传入
     * {@link java.time.LocalDateTime}.class 即可透明解析。</p>
     * <p>账号被临时锁定时填入，永久锁定或未锁定时为 {@code null}。</p>
     */
    String LOCKED_UNTIL = "lockedUntil";

    /**
     * 当前已连续失败次数（{@link Integer}）。
     * 账号 locked=false 时有意义；不提供时不做详细密码错误提示。
     */
    String FAILED_LOGIN_COUNT = "failedLoginCount";

    /**
     * 触发自动锁定的阈值（{@link Integer}）。
     * 0 或缺省表示不启用自动锁定，此时不做详细密码错误提示。
     */
    String MAX_FAILED_ATTEMPTS = "maxFailedAttempts";

    /**
     * 从第几次失败开始给出"还剩几次将锁定"的详细提示（{@link Integer}）。
     * 默认 3；之前的失败按通用提示返回。
     */
    String HINT_AFTER_ATTEMPTS = "hintAfterAttempts";
}
