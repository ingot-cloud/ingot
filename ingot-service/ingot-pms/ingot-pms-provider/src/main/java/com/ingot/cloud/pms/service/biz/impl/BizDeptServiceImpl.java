package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Description  : BizDeptServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/18.</p>
 * <p>Time         : 4:27 PM.</p>
 */
@Service
@RequiredArgsConstructor
public class BizDeptServiceImpl implements BizDeptService {
    private final SysDeptService sysDeptService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserDeptsEnsureMainDept(long userId, List<Long> deptIds) {
        SysDept main = sysDeptService.getMainDept();

        if (CollUtil.isEmpty(deptIds)) {
            deptIds = ListUtil.list(false, main.getId());
        } else {
            deptIds.add(main.getId());
        }

        Set<Long> temp = new HashSet<>(deptIds);
        sysDeptService.setDepts(userId, temp.stream().toList());
    }
}
