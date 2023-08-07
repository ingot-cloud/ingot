package com.ingot.cloud.pms.common;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Description  : CommonRoleRelationService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/31.</p>
 * <p>Time         : 9:59 上午.</p>
 */
@Slf4j
public class CommonRoleRelationService<M extends BaseMapper<T>, T, TargetID> extends BaseServiceImpl<M, T> {
    protected AssertionChecker assertionChecker;

    @Autowired
    public void setAssertI18nService(AssertionChecker assertionChecker) {
        this.assertionChecker = assertionChecker;
    }

    /**
     * 目标类型关联角色
     */
    public void bindRoles(RelationDTO<TargetID, Long> params,
                          Do<TargetID> remove,
                          Do<TargetID> bind,
                          String removeErrorMsgCode) {
        TargetID id = params.getId();
        List<Long> removeIds = params.getRemoveIds();
        List<Long> bindIds = params.getBindIds();

        if (CollUtil.isNotEmpty(removeIds)) {
            log.debug("[CommonRoleRelationService] - 目标[{}]绑定角色 取消关联[{}]", id, removeIds);
            boolean removeRet = removeIds.stream().allMatch(roleId -> remove.exec(roleId, id));
            assertionChecker.checkOperation(removeRet, removeErrorMsgCode);
        }

        if (CollUtil.isNotEmpty(bindIds)) {
            log.debug("[CommonRoleRelationService] - 目标[{}]绑定角色 关联[{}]", id, bindIds);
            bindIds.forEach(roleId -> bind.exec(roleId, id));
        }
    }

    /**
     * 角色关联目标
     */
    public void bindTargets(RelationDTO<Long, TargetID> params,
                            Do<TargetID> remove,
                            Do<TargetID> bind,
                            String removeErrorMsgCode) {
        Long id = params.getId();
        List<TargetID> removeIds = params.getRemoveIds();
        List<TargetID> bindIds = params.getBindIds();

        if (CollUtil.isNotEmpty(removeIds)) {
            log.debug("[CommonRoleRelationService] - 角色[{}]绑定目标 取消关联[{}]", id, removeIds);
            boolean removeRet = removeIds.stream().allMatch(targetId -> remove.exec(id, targetId));
            assertionChecker.checkOperation(removeRet, removeErrorMsgCode);
        }

        if (CollUtil.isNotEmpty(bindIds)) {
            log.debug("[CommonRoleRelationService] - 角色[{}]绑定目标 关联[{}]", id, bindIds);
            bindIds.forEach(targetId -> bind.exec(id, targetId));
        }
    }

    @FunctionalInterface
    public interface Do<TargetID> {
        boolean exec(Long roleId, TargetID targetId);
    }
}
