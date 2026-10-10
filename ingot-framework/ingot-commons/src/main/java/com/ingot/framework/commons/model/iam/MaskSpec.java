package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

/**
 * <p>不可变的文本脱敏参数；区间按 Unicode code point 的零起始、左闭右开位置解释。</p>
 * @author jy
 * @since 1.0.0
 * @param kind 脱敏类型
 * @param prefix 保留头部长度，仅 KEEP_EDGES 使用
 * @param suffix 保留尾部长度，仅 KEEP_EDGES 使用
 * @param start 遮盖开始位置，仅 RANGE 使用
 * @param end 遮盖结束位置，仅 RANGE 使用
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaskSpec(@NotNull MaskKind kind, Integer prefix, Integer suffix, Integer start, Integer end) {
    /** 可配置位置的最大值，避免无界参数。 */
    public static final int MAX_POSITION = 256;
    /** 不透出任何原值的默认规则。 */
    public static final MaskSpec ALL = new MaskSpec(MaskKind.ALL, null, null, null, null);
    /** 常规手机号脱敏规则。 */
    public static final MaskSpec PHONE = new MaskSpec(MaskKind.PHONE, null, null, null, null);
    /** 常规邮箱脱敏规则。 */
    public static final MaskSpec EMAIL = new MaskSpec(MaskKind.EMAIL, null, null, null, null);

    /** 拒绝缺失、互斥或无效参数，不把非法规则当成完整展示。 */
    public MaskSpec {
        if (kind == null) throw new IllegalArgumentException("脱敏类型不能为空");
        boolean valid = switch (kind) {
            case KEEP_EDGES -> position(prefix) && position(suffix) && start == null && end == null;
            case RANGE -> position(start) && position(end) && end > start && prefix == null && suffix == null;
            default -> prefix == null && suffix == null && start == null && end == null;
        };
        if (!valid) throw new IllegalArgumentException("脱敏参数不合法");
    }

    private static boolean position(Integer value) {
        return value != null && value >= 0 && value <= MAX_POSITION;
    }
}
