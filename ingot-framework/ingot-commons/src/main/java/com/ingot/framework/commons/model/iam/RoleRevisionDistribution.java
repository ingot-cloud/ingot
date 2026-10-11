package com.ingot.framework.commons.model.iam;

/**
 * <p>当前可治理且未撤销、未到期的分配版本分布，含未来生效记录。</p>
 * @param revisionId 固定版本ID
 * @param revision 系统版本号
 * @param versionName 可读名称
 * @param count 同可见性边界的分配数
 * @author jy
 * @since 1.0.0
 */
public record RoleRevisionDistribution(String revisionId, String revision, String versionName,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = IamCountSerializer.class) Long count) {
}
