package com.ingot.cloud.iam.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>同一精确操作在全部有效授权上的范围并集，空集合表示有操作但无对象。</p>
 *
 * @param clauses 可满足任一条款即落入范围
 * @author jy
 * @since 1.0.0
 */
public record ResolvedActionScope(List<ScopeClause> clauses) {
    /**
     * 复制条款列表。
     */
    public ResolvedActionScope {
        clauses = clauses == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(clauses));
    }

    /**
     * 构造无对象范围。
     *
     * @return 空范围
     */
    public static ResolvedActionScope empty() {
        return new ResolvedActionScope(List.of());
    }

    /**
     * 判断是否不覆盖任何对象。
     *
     * @return 无有效条款时为 true
     */
    public boolean isEmpty() {
        return clauses.isEmpty() || clauses.stream().allMatch(ScopeClause::empty);
    }
}
