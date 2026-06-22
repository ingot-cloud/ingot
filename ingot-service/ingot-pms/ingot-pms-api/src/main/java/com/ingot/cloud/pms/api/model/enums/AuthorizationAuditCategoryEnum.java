package com.ingot.cloud.pms.api.model.enums;

/**
 * <p>授权数据审计问题分类，对应一类可定位的完整性缺陷。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum AuthorizationAuditCategoryEnum {
    INVALID_MENU_PARENT,
    MENU_PERMISSION_MISMATCH,
    MENU_PERMISSION_PARENT_MISMATCH,
    APP_ROOT_REFERENCE_MISSING,
    DUPLICATE_PERMISSION_CODE,
    ROLE_PERMISSION_ORPHAN,
    TENANT_PRIVATE_RELATION_INCOMPLETE,
    ROLE_SOURCE_FLAG_MISMATCH
}
