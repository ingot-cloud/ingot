package com.ingot.framework.commons.model.iam;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>调整待处理集合的部分参数；未覆盖的兼容绑定保留。</p>
 *
 * @param expectedVersion 计划乐观锁
 * @param all 明确选择全部待处理记录
 * @param itemIds 选定明细ID
 * @param scopeBindings 明确覆盖的参数
 * @param excluded 排除或重新纳入选中记录，可空
 * @author jy
 * @since 1.0.0
 */
public record RoleApplicationChangeInput(
        @NotBlank String expectedVersion,
        boolean all,
        List<@NotBlank String> itemIds,
        Map<@NotBlank String, @NotNull @Valid ScopeBinding> scopeBindings,
        Boolean excluded) {
}
