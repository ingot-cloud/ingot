package com.ingot.cloud.iam.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>求值后的类型化对象范围，列表、计数、详情和导出使用同一谓词，不拼接 SQL。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ObjectScope {
    private final boolean all;
    private final List<ObjectScopeClause> clauses;

    private ObjectScope(boolean all, List<ObjectScopeClause> clauses) {
        this.all = all;
        this.clauses = clauses;
    }

    /**
     * 构造不覆盖任何对象的范围。
     *
     * @return 空范围
     */
    public static ObjectScope none() {
        return new ObjectScope(false, List.of());
    }

    /**
     * 构造覆盖当前域全部对象的范围，仍须由调用方绑定可信租户。
     *
     * @return 全域范围
     */
    public static ObjectScope all() {
        return new ObjectScope(true, List.of());
    }

    /**
     * 由已展开条款构造范围；空列表视为无对象。
     *
     * @param clauses 可满足任一条款即落入范围
     * @return 过滤范围
     */
    public static ObjectScope of(List<ObjectScopeClause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return none();
        }
        return new ObjectScope(false, Collections.unmodifiableList(new ArrayList<>(clauses)));
    }

    /**
     * 判断是否覆盖当前域全部对象。
     *
     * @return 全域时为 true
     */
    public boolean coversAll() {
        return all;
    }

    /**
     * 判断是否不覆盖任何对象。
     *
     * @return 无有效条款且非全域时为 true
     */
    public boolean coversNone() {
        return !all && clauses.isEmpty();
    }

    /**
     * 返回过滤条款；全域或空范围时为空列表。
     *
     * @return 不可变条款
     */
    public List<ObjectScopeClause> clauses() {
        return clauses;
    }
}
