package com.ingot.framework.commons.model.iam;

/**
 * <p>按字段可见程度投影输出值，隐藏省略，脱敏只返回占位符，完整才给原值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class FieldProjection {
    /**
     * 脱敏输出及客户端回传时必须拒绝的占位符。
     */
    public static final String MASKED_PLACEHOLDER = "***";

    private FieldProjection() {
    }

    /**
     * 将存储原值投影为响应值；隐藏返回空引用。
     *
     * @param value 存储原值
     * @param visibility 可见程度
     * @return 可写入响应的值
     */
    public static String project(String value, FieldVisibility visibility) {
        if (value == null || visibility == null || visibility == FieldVisibility.HIDDEN) {
            return null;
        }
        if (visibility == FieldVisibility.MASKED) {
            return MASKED_PLACEHOLDER;
        }
        return value;
    }

    /**
     * 判断客户端提交是否为脱敏占位，不能当作原值写入。
     *
     * @param value 提交值
     * @return 占位符时为 true
     */
    public static boolean maskedPlaceholder(String value) {
        return MASKED_PLACEHOLDER.equals(value);
    }
}
