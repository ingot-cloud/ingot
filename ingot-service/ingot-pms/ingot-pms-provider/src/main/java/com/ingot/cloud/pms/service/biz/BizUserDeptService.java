package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.framework.security.core.context.SecurityAuthContext;

/**
 * <p>Description  : 用户部门相关业务服务。</p>
 * <p>对外提供基于当前登录用户（{@link SecurityAuthContext#getUser()}）或指定用户的部门查询方法，
 * 底层依赖的 {@code tenant_user_dept_private} 关系表查询使用缓存以提升性能，
 * 当用户部门关系发生变化时会自动清理缓存。</p>
 *
 * @author jy
 * @since 2026-05-19
 */
public interface BizUserDeptService {

    /**
     * 获取当前登录用户所属的部门ID列表
     *
     * @return 部门ID列表
     */
    List<Long> getDeptIds();

    /**
     * 获取指定用户所属的部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<Long> getDeptIds(long userId);

    /**
     * 获取当前登录用户所属的部门信息列表
     *
     * @return 部门列表
     */
    List<TenantDept> getDepts();

    /**
     * 获取指定用户所属的部门信息列表
     *
     * @param userId 用户ID
     * @return 部门列表
     */
    List<TenantDept> getDepts(long userId);

    /**
     * 判断当前登录用户是否属于指定部门
     *
     * @param deptId 部门ID
     * @return true: 属于该部门
     */
    boolean inDept(long deptId);

    /**
     * 判断指定用户是否属于指定部门
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return true: 属于该部门
     */
    boolean inDept(long userId, long deptId);

    /**
     * 获取当前登录用户所属部门及其所有子部门列表
     *
     * @param includeSelf 是否包含用户所属的部门自身
     * @return 部门列表
     */
    List<TenantDept> getDescendantDepts(boolean includeSelf);

    /**
     * 获取指定用户所属部门及其所有子部门列表
     *
     * @param userId      用户ID
     * @param includeSelf 是否包含用户所属的部门自身
     * @return 部门列表
     */
    List<TenantDept> getDescendantDepts(long userId, boolean includeSelf);

    /**
     * 获取当前登录用户所属部门及其所有子部门ID列表（已去重）
     *
     * @param includeSelf 是否包含用户所属的部门自身
     * @return 部门ID列表
     */
    List<Long> getDescendantDeptIds(boolean includeSelf);

    /**
     * 获取指定用户所属部门及其所有子部门ID列表（已去重）
     *
     * @param userId      用户ID
     * @param includeSelf 是否包含用户所属的部门自身
     * @return 部门ID列表
     */
    List<Long> getDescendantDeptIds(long userId, boolean includeSelf);

    /**
     * 设置当前登录用户的部门关联（会触发缓存清理）
     *
     * @param deptIds 部门ID列表
     */
    void setDepts(List<Long> deptIds);

    /**
     * 设置指定用户的部门关联（会触发缓存清理）
     *
     * @param userId  用户ID
     * @param deptIds 部门ID列表
     */
    void setDepts(long userId, List<Long> deptIds);

    /**
     * 清空当前登录用户的部门关联（会触发缓存清理）
     */
    void clear();

    /**
     * 清空指定用户的部门关联（会触发缓存清理）
     *
     * @param userId 用户ID
     */
    void clear(long userId);
}
