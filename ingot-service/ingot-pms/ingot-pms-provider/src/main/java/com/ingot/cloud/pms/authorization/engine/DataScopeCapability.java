package com.ingot.cloud.pms.authorization.engine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;

/**
 * <p>授予者在某一 {@code (permission, resource)} 上可证明的数据范围并集。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DataScopeCapability {

    private boolean all;
    private boolean self;
    private final Set<Long> deptIds = new HashSet<>();

    /**
     * 合并 ALL 范围。
     */
    public void mergeAll() {
        this.all = true;
    }

    /**
     * 合并授予者本人的 SELF 范围，不能直接转授给其他用户。
     */
    public void mergeSelf() {
        this.self = true;
    }

    /**
     * 合并可覆盖的部门 ID。
     *
     * @param ids 部门 ID，可空
     */
    public void mergeDepts(Collection<Long> ids) {
        if (CollUtil.isNotEmpty(ids)) {
            deptIds.addAll(ids);
        }
    }

    /**
     * 判断拟授予规则是否可证明为当前能力的子集。
     *
     * @param proposed          拟授予范围类型
     * @param proposedDeptIds   CUSTOM 部门；其它类型可空
     * @param bindDeptId        拟绑定部门，非部门角色为 {@code null}
     * @param bindDeptAndChild  拟绑定部门及其子孙，DEPT_AND_CHILD 时使用
     * @param grantorUserId     授予者用户 ID
     * @param recipientUserIds  接收用户，设置规则尚未绑定时可空
     * @return 能证明为子集时返回 {@code true}
     */
    public boolean covers(DataScopeTypeEnum proposed,
                          Collection<Long> proposedDeptIds,
                          Long bindDeptId,
                          Collection<Long> bindDeptAndChild,
                          long grantorUserId,
                          Collection<Long> recipientUserIds) {
        if (proposed == null) {
            return false;
        }
        if (all) {
            return true;
        }
        return switch (proposed) {
            case ALL -> false;
            case DEPT -> bindDeptId != null && deptIds.contains(bindDeptId);
            case DEPT_AND_CHILD -> CollUtil.isNotEmpty(bindDeptAndChild)
                    && deptIds.containsAll(bindDeptAndChild);
            case CUSTOM -> CollUtil.isNotEmpty(proposedDeptIds)
                    && deptIds.containsAll(proposedDeptIds);
            case SELF -> coversSelf(bindDeptId, grantorUserId, recipientUserIds);
        };
    }

    private boolean coversSelf(Long bindDeptId, long grantorUserId, Collection<Long> recipientUserIds) {
        if (CollUtil.isEmpty(recipientUserIds)) {
            return false;
        }
        boolean onlySelf = recipientUserIds.stream().allMatch(id -> id != null && id == grantorUserId);
        if (onlySelf) {
            return self || (bindDeptId != null && deptIds.contains(bindDeptId));
        }
        return bindDeptId != null && deptIds.contains(bindDeptId);
    }
}
