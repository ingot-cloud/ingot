package com.ingot.cloud.pms.common;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.store.mybatis.mapper.BaseMapper;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Description  : CommonRoleRelationService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/31.</p>
 * <p>Time         : 9:59 上午.</p>
 */
public class CommonRoleRelationService<M extends BaseMapper<T>, T> extends BaseServiceImpl<M, T> {
    private static final int TYPE_ROLE = 1;
    private static final int TYPE_TARGET = 2;

    protected AssertI18nService assertI18nService;

    @Autowired
    public void setAssertI18nService(AssertI18nService assertI18nService) {
        this.assertI18nService = assertI18nService;
    }

    /**
     * 目标类型关联角色
     */
    public void bindRoles(RelationDTO<Integer, Integer> params,
                          Do remove,
                          Do bind,
                          String removeErrorMsgCode) {
        bind(TYPE_TARGET, params, remove, bind, removeErrorMsgCode);
    }

    /**
     * 角色关联目标
     */
    public void bindTargets(RelationDTO<Integer, Integer> params,
                            Do remove,
                            Do bind,
                            String removeErrorMsgCode) {
        bind(TYPE_ROLE, params, remove, bind, removeErrorMsgCode);
    }

    private void bind(int type,
                      RelationDTO<Integer, Integer> params,
                      Do remove,
                      Do bind,
                      String removeErrorMsgCode) {
        Integer id = params.getId();
        List<Integer> removeIds = params.getRemoveIds();
        List<Integer> bindIds = params.getBindIds();

        if (CollUtil.isNotEmpty(removeIds)) {
            boolean removeRet = true;
            switch (type) {
                case TYPE_ROLE:
                    removeRet = removeIds.stream().allMatch(targetId -> remove.exec(id, targetId));
                    break;
                case TYPE_TARGET:
                    removeRet = removeIds.stream().allMatch(roleId -> remove.exec(roleId, id));
                    break;
            }
            assertI18nService.checkOperation(removeRet, removeErrorMsgCode);
        }

        if (CollUtil.isNotEmpty(bindIds)) {
            switch (type) {
                case TYPE_ROLE:
                    bindIds.forEach(targetId -> bind.exec(id, targetId));
                    break;
                case TYPE_TARGET:
                    bindIds.forEach(roleId -> bind.exec(roleId, id));
                    break;
            }
        }
    }

    @FunctionalInterface
    public interface Do {
        boolean exec(long roleId, long targetId);
    }
}
