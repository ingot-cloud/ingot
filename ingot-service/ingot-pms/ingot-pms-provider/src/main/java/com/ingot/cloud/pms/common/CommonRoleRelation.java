package com.ingot.cloud.pms.common;

import cn.hutool.core.collection.CollUtil;
import com.ingot.framework.core.model.dto.common.RelationDto;
import com.ingot.framework.core.validation.service.AssertI18nService;

import java.util.List;

/**
 * <p>Description  : CommonRoleRelation.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/31.</p>
 * <p>Time         : 9:12 上午.</p>
 */
public class CommonRoleRelation {
    public static final int TYPE_ROLE = 1;
    public static final int TYPE_TARGET = 2;

    public static void bind(int type,
                            RelationDto<Long, Long> params,
                            Do remove,
                            Do bind,
                            AssertI18nService assertI18nService,
                            String removeErrorMsgCode) {
        Long id = params.getId();
        List<Long> removeIds = params.getRemoveIds();
        List<Long> bindIds = params.getBindIds();

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
