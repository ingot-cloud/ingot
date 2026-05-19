package com.ingot.cloud.pms.service.biz.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.biz.BizUserDeptService;
import com.ingot.cloud.pms.service.domain.TenantDeptService;
import com.ingot.cloud.pms.service.domain.TenantUserDeptPrivateService;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : {@link BizUserDeptService} 实现。</p>
 * <p>无参方法默认使用 {@link SecurityAuthContext#getUser()} 获取当前登录用户ID，
 * 因此必须在已认证的请求上下文中调用，否则抛出业务异常。</p>
 *
 * @author jy
 * @since 2026-05-19
 */
@Service
@RequiredArgsConstructor
public class BizUserDeptServiceImpl implements BizUserDeptService {

    private final TenantDeptService tenantDeptService;
    private final TenantUserDeptPrivateService tenantUserDeptPrivateService;
    private final BizDeptService bizDeptService;
    private final AssertionChecker assertionChecker;

    @Override
    public List<Long> getDeptIds() {
        return getDeptIds(currentUserId());
    }

    @Override
    public List<Long> getDeptIds(long userId) {
        return CollUtil.emptyIfNull(tenantUserDeptPrivateService.getUserDepartmentIds(userId));
    }

    @Override
    public List<TenantDept> getDepts() {
        return getDepts(currentUserId());
    }

    @Override
    public List<TenantDept> getDepts(long userId) {
        List<Long> deptIds = getDeptIds(userId);
        if (CollUtil.isEmpty(deptIds)) {
            return ListUtil.empty();
        }
        return CollUtil.emptyIfNull(tenantDeptService.listByIds(deptIds));
    }

    @Override
    public boolean inDept(long deptId) {
        return inDept(currentUserId(), deptId);
    }

    @Override
    public boolean inDept(long userId, long deptId) {
        return getDeptIds(userId).stream().anyMatch(id -> Objects.equals(id, deptId));
    }

    @Override
    public List<TenantDept> getDescendantDepts(boolean includeSelf) {
        return getDescendantDepts(currentUserId(), includeSelf);
    }

    @Override
    public List<TenantDept> getDescendantDepts(long userId, boolean includeSelf) {
        List<Long> deptIds = getDeptIds(userId);
        if (CollUtil.isEmpty(deptIds)) {
            return ListUtil.empty();
        }
        // 用户可能挂在多个部门下，且这些部门可能存在父子关系，需要按 ID 去重
        Map<Long, TenantDept> deduplicated = new LinkedHashMap<>();
        deptIds.forEach(deptId -> bizDeptService.getDescendantList(deptId, includeSelf)
                .forEach(dept -> deduplicated.putIfAbsent(dept.getId(), dept)));
        return List.copyOf(deduplicated.values());
    }

    @Override
    public List<Long> getDescendantDeptIds(boolean includeSelf) {
        return getDescendantDeptIds(currentUserId(), includeSelf);
    }

    @Override
    public List<Long> getDescendantDeptIds(long userId, boolean includeSelf) {
        return getDescendantDepts(userId, includeSelf).stream()
                .map(TenantDept::getId)
                .toList();
    }

    @Override
    public void setDepts(List<Long> deptIds) {
        setDepts(currentUserId(), deptIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDepts(long userId, List<Long> deptIds) {
        // 用户不能直接挂到主部门，需要剔除
        Set<Long> targets = deptIds == null ? new HashSet<>() : new HashSet<>(deptIds);
        if (!targets.isEmpty()) {
            TenantDept main = tenantDeptService.getMainDept();
            if (main != null) {
                targets.remove(main.getId());
            }
        }
        // 底层 TenantUserDeptPrivateService 已通过 @CacheEvict 清理缓存
        tenantUserDeptPrivateService.setDepartments(userId, targets);
    }

    @Override
    public void clear() {
        clear(currentUserId());
    }

    @Override
    public void clear(long userId) {
        tenantUserDeptPrivateService.clearByUserId(userId);
    }

    /**
     * 获取当前登录用户ID
     */
    private long currentUserId() {
        InUser user = SecurityAuthContext.getUser();
        assertionChecker.checkOperation(user != null && user.getId() != null,
                "BizUserDeptServiceImpl.UnAuthenticated");
        return user.getId();
    }
}
