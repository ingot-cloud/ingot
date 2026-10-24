package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>授权审计键集游标，只编码 {@code (occurred_at, id)} 排序位置，不携带域、租户或筛选条件。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param occurredAt UTC 发生时间
 * @param id 审计 ID
 */
public record AuditCursor(LocalDateTime occurredAt, BigInteger id) {
    /** 编码后游标的最大长度。 */
    public static final int MAX_LENGTH = 64;
    private static final long MICROS_PER_SECOND = 1_000_000L;
    private static final long NANOS_PER_MICRO = 1_000L;
    private static final Pattern PLAIN = Pattern.compile("([0-9]{1,19})\\.([1-9][0-9]{0,19})");

    /**
     * 编码为不透明的 base64url 文本。
     *
     * @return 游标文本
     */
    public String encode() {
        long micros = occurredAt.toEpochSecond(ZoneOffset.UTC) * MICROS_PER_SECOND
                + occurredAt.getNano() / NANOS_PER_MICRO;
        String plain = micros + "." + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(plain.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * 解码客户端回传的游标。
     *
     * @param value 游标文本，空表示第一页
     * @return 游标；空输入返回 {@code null}
     * @throws BizException 格式非法时为 InvalidArgument
     */
    public static AuditCursor decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() > MAX_LENGTH) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        String plain;
        try {
            plain = new String(Base64.getUrlDecoder().decode(value.trim()), StandardCharsets.US_ASCII);
        } catch (IllegalArgumentException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        Matcher matcher = PLAIN.matcher(plain);
        if (!matcher.matches()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        try {
            long micros = Long.parseLong(matcher.group(1));
            LocalDateTime time = LocalDateTime.ofEpochSecond(Math.floorDiv(micros, MICROS_PER_SECOND), 0, ZoneOffset.UTC)
                    .plus(Math.floorMod(micros, MICROS_PER_SECOND), ChronoUnit.MICROS);
            return new AuditCursor(time, new BigInteger(matcher.group(2)));
        } catch (RuntimeException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }
}
