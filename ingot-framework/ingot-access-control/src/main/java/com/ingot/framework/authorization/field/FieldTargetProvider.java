package com.ingot.framework.authorization.field;

import java.util.Collection;
import java.util.Map;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;

/**
 * <p>业务适配器一次批量加载真实归属，避免序列化阶段逐字段或逐行查询。</p>
 * @param <T> 业务 DTO 类型
 * @author jy
 * @since 1.0.0
 */
@FunctionalInterface
public interface FieldTargetProvider<T> {
    /** 按对象标识返回本批 DTO 的可信对象事实；不可采用浏览器声明的归属。 */
    Map<String, ScopeTarget> load(Collection<T> values);
}
