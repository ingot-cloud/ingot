package com.ingot.cloud.iam.authorization.migration;

/**
 * <p>已批准的映射动作，供 dry-run 对照与 apply 执行。</p>
 *
 * @param type    动作类型
 * @param subject 对象标识
 * @param detail  说明
 * @author jy
 * @since 1.0.0
 */
public record MigrationMappedChange(
        String type,
        String subject,
        String detail
) {
    public static final String MENU_PERMISSION_LINK = "MENU_PERMISSION_LINK";
    public static final String CREATE_VIEW_PERMISSION = "CREATE_VIEW_PERMISSION";
    public static final String REMOVE_BUTTON_MENU = "REMOVE_BUTTON_MENU";
    public static final String WILDCARD_TO_ANT = "WILDCARD_TO_ANT";
    public static final String APP_DEFAULT_OPEN = "APP_DEFAULT_OPEN";
}
