package com.ingot.framework.commons.model.iam;

/**
 * <p>当前平台身份的直接分配资格与受限入口；委派能力不计入直接资格。</p>
 * @param directRead 可直接读取全域分配
 * @param directCreate 可不带来源直接分配
 * @param directUpdate 可调整全域分配
 * @param directRevoke 可撤销全域分配
 * @param effectiveDelegationCount 本人当前有效委派数
 * @author jy
 * @since 1.0.0
 */
public record AssignmentContext(boolean directRead, boolean directCreate, boolean directUpdate,
                                boolean directRevoke,
                                @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = IamCountSerializer.class)
                                long effectiveDelegationCount) {
}
