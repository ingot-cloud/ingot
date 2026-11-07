package com.ingot.cloud.iam.infrastructure.persistence.projection;

import java.math.BigInteger;

/**
 * <p>承载可用人群绑定行与目标名称的连接查询，不按父表 ID 列表回查。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class AudienceBindingRows {
    private AudienceBindingRows() {
    }

    /**
     * <p>一名已绑定成员及其显示名、头像。目标缺失时名称为空。</p>
     *
     * @param id 成员 ID
     * @param name 显示名称，可空
     * @param avatar 库存头像路径，可空
     * @author jy
     * @since 1.0.0
     */
    public record Member(BigInteger id, String name, String avatar) {
    }

    /**
     * <p>一个已绑定部门及其名称和是否含下级。目标缺失时名称为空。</p>
     *
     * @param id 部门 ID
     * @param name 部门名称，可空
     * @param includeDescendants 绑定行上的含下级标记
     * @author jy
     * @since 1.0.0
     */
    public record Department(BigInteger id, String name, Boolean includeDescendants) {
    }

    /**
     * <p>一个已绑定用户组及其名称。目标缺失时名称为空。</p>
     *
     * @param id 用户组 ID
     * @param name 组名称，可空
     * @author jy
     * @since 1.0.0
     */
    public record Group(BigInteger id, String name) {
    }
}
