package com.ingot.framework.authorization.field;

import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.MaskSpec;

/**
 * <p>以 Unicode code point 执行预设或参数化规则；短值和异常格式全部遮盖。</p>
 * @author jy
 * @since 1.0.0
 */
public final class DefaultMaskStrategy implements MaskStrategy {
    private static final int PHONE_PREFIX = 3;
    private static final int PHONE_SUFFIX = 4;
    private static final String MASK_CHAR = "*";
    private static final char EMAIL_SEPARATOR = '@';

    /** 不使用原值作为规则失败时的回退结果。 */
    @Override
    public String mask(String value, MaskSpec spec) {
        if (value == null) return null;
        if (spec == null) spec = MaskSpec.ALL;
        int length = value.codePointCount(0, value.length());
        return switch (spec.kind()) {
            case ALL -> FieldProjection.MASKED_PLACEHOLDER;
            case PHONE -> value.codePoints().allMatch(Character::isDigit)
                    ? edges(value, length, PHONE_PREFIX, PHONE_SUFFIX) : FieldProjection.MASKED_PLACEHOLDER;
            case EMAIL -> email(value);
            case KEEP_EDGES -> edges(value, length, spec.prefix(), spec.suffix());
            case RANGE -> range(value, length, spec.start(), spec.end());
        };
    }

    private static String email(String value) {
        int index = value.indexOf(EMAIL_SEPARATOR);
        if (value.codePoints().anyMatch(code -> Character.isWhitespace(code) || Character.isISOControl(code)) || index <= 0 || index == value.length() - 1 || index != value.lastIndexOf(EMAIL_SEPARATOR))
            return FieldProjection.MASKED_PLACEHOLDER;
        String local = value.substring(0, index);
        int first = local.offsetByCodePoints(0, 1);
        return (local.codePointCount(0, local.length()) > 1 ? local.substring(0, first) : "")
                + FieldProjection.MASKED_PLACEHOLDER + value.substring(index);
    }

    private static String edges(String value, int length, int prefix, int suffix) {
        if (length <= prefix + suffix) return FieldProjection.MASKED_PLACEHOLDER;
        return value.substring(0, value.offsetByCodePoints(0, prefix))
                + MASK_CHAR.repeat(length - prefix - suffix)
                + value.substring(value.offsetByCodePoints(0, length - suffix));
    }

    private static String range(String value, int length, int start, int end) {
        if (start >= length) return FieldProjection.MASKED_PLACEHOLDER;
        int actualEnd = Math.min(length, end);
        return value.substring(0, value.offsetByCodePoints(0, start)) + MASK_CHAR.repeat(actualEnd - start)
                + value.substring(value.offsetByCodePoints(0, actualEnd));
    }
}
