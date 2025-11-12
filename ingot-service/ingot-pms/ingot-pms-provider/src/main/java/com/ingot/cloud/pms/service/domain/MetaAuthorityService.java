package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface MetaAuthorityService extends BaseService<MetaAuthority> {
    /**
     * 创建权限
     *
     * @param authority {@link MetaAuthority}
     * @param fillParentCode 是否填充父级权限编码
     */
    void create(MetaAuthority authority, boolean fillParentCode);

    /**
     * 创建权限并且返回权限ID
     *
     * @param authority {@link MetaAuthority}
     * @param fillParentCode 是否填充父级权限编码
     * @return ID
     */
    Long createAndReturnId(MetaAuthority authority, boolean fillParentCode);

    /**
     * 修改权限
     *
     * @param authority {@link MetaAuthority}
     */
    void update(MetaAuthority authority);

    /**
     * 删除权限，只单纯做权限删除，不进行相关关系清理
     *
     * @param id 权限ID
     */
    void delete(long id);
}
