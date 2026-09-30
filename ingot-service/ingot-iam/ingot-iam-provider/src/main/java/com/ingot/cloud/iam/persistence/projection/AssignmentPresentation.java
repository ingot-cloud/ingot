package com.ingot.cloud.iam.persistence.projection;

/**
 * <p>批量关联的角色分配显示信息，创建审计只选择最早 CREATE。</p>
 * @param id 分配 ID
 * @param subjectName 接收对象
 * @param roleName 角色
 * @param revisionNumber 版本
 * @param authorId 实际授权人
 * @param authorName 实际授权人名称
 * @param sourceValid 当前来源有效性
 * @author jy
 * @since 1.0.0
 */
public record AssignmentPresentation(java.math.BigInteger id, String subjectName, String roleName,
        java.math.BigInteger revisionNumber, java.math.BigInteger authorId, String authorName, boolean sourceValid) { }
