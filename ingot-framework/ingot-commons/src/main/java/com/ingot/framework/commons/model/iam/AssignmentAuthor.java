package com.ingot.framework.commons.model.iam;

/**
 * <p>创建审计中的实际授权人；历史无审计不推测授权人。</p>
 * @param memberId 实际授权成员，可空
 * @param name 授权人显示名，历史缺失为未知
 * @author jy
 * @since 1.0.0
 */
public record AssignmentAuthor(String memberId, String name) { }
