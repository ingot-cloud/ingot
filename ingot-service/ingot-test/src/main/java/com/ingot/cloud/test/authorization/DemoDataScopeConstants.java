package com.ingot.cloud.test.authorization;

/**
 * <p>示例资源与操作权限编码，供联调订单/公告数据范围使用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DemoDataScopeConstants {

    /**
     * 订单资源编码。
     */
    public static final String RESOURCE_ORDER = "demo-order";

    /**
     * 公告资源编码。
     */
    public static final String RESOURCE_ANNOUNCEMENT = "demo-announcement";

    /**
     * 学生示例资源编码。
     */
    public static final String RESOURCE_STUDENT = "t_student";

    /**
     * 订单查询权限。
     */
    public static final String PERM_ORDER_QUERY = "demo:order:query";

    /**
     * 订单创建权限。
     */
    public static final String PERM_ORDER_CREATE = "demo:order:create";

    /**
     * 订单更新权限。
     */
    public static final String PERM_ORDER_UPDATE = "demo:order:update";

    /**
     * 订单删除权限。
     */
    public static final String PERM_ORDER_DELETE = "demo:order:delete";

    /**
     * 公告查询权限。
     */
    public static final String PERM_ANNOUNCEMENT_QUERY = "demo:announcement:query";

    /**
     * 公告创建权限。
     */
    public static final String PERM_ANNOUNCEMENT_CREATE = "demo:announcement:create";

    /**
     * 公告更新权限。
     */
    public static final String PERM_ANNOUNCEMENT_UPDATE = "demo:announcement:update";

    /**
     * 公告删除权限。
     */
    public static final String PERM_ANNOUNCEMENT_DELETE = "demo:announcement:delete";

    /**
     * 学生查询权限。
     */
    public static final String PERM_STUDENT_QUERY = "demo:student:query";

    private DemoDataScopeConstants() {
    }
}
