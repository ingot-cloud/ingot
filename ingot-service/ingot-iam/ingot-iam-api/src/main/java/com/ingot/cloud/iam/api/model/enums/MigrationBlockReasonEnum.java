package com.ingot.cloud.iam.api.model.enums;

/**
 * <p>授权迁移阻断类别，出现任一项则拒绝 apply，不得猜测修复。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum MigrationBlockReasonEnum {
    /** 同一租户用户角色在规范化部门上存在重复任职。 */
    DUPLICATE_DEPT_BINDING,
    /** 需要新建页面 view 权限但编码已被占用。 */
    VIEW_CODE_COLLISION,
    /** 菜单或权限缺少所属应用。 */
    MISSING_APP_ID,
    /** 迁移快照中菜单旧 permission_id 悬空。 */
    UNKNOWN_SOURCE,
    /** 角色绑定或规则引用了不存在的权限、资源或部门。 */
    DANGLING_RELATION,
    /** 任职指向其它租户的部门。 */
    CROSS_TENANT_DEPT,
    /** 022 dry-run 在 DROP 角色列前报告：平台角色遗留 CUSTOM 无法拆到唯一租户。 */
    PLATFORM_CUSTOM_TENANT_DEPT,
    /** 旧 {@code :*} 转为 {@code :**} 后与已有编码冲突。 */
    WILDCARD_CODE_COLLISION
}
