package com.ingot.cloud.member.service.domain;

import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
public interface MemberPermissionService extends BaseService<MemberPermission> {
    /**
     * 创建权限
     *
     * @param authority      {@link MemberPermission}
     * @param fillParentCode 是否填充父级权限编码
     */
    void create(MemberPermission authority, boolean fillParentCode);

    /**
     * 创建权限并且返回权限ID
     *
     * @param authority      {@link MemberPermission}
     * @param fillParentCode 是否填充父级权限编码
     * @return ID
     */
    Long createAndReturnId(MemberPermission authority, boolean fillParentCode);

    /**
     * 修改权限
     *
     * @param authority {@link MemberPermission}
     */
    void update(MemberPermission authority);

    /**
     * 删除权限，只单纯做权限删除，不进行相关关系清理
     *
     * @param id 权限ID
     */
    void delete(long id);
}
