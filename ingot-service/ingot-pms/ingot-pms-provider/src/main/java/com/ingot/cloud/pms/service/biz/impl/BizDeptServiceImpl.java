package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
    private final AssertionChecker assertionChecker;

    @Override
    public List<DeptTreeNodeVO> orgList() {
        return sysDeptService.treeList();
    }

    @Override
    public void orgCreateDept(SysDept params) {
        assertionChecker.checkOperation(params.getPid() == null,
                "BizDeptServiceImpl.createError");
        sysDeptService.createDept(params);
    }

    @Override
    public void orgUpdateDept(SysDept params) {
        SysDept main = sysDeptService.getMainDept();
        assertionChecker.checkOperation(!Objects.equals(params.getId(), main.getId()),
                "BizDeptServiceImpl.updateError");

        sysDeptService.updateDept(params);
    }

    @Override
    public void orgDeleteDept(long id) {
        SysDept main = sysDeptService.getMainDept();
        assertionChecker.checkOperation(id != main.getId(),
                "BizDeptServiceImpl.deleteError");
        sysDeptService.removeDeptById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserDeptsEnsureMainDept(long userId, List<Long> deptIds) {
        SysDept main = sysDeptService.getMainDept();
        if (main != null) {
            if (CollUtil.isEmpty(deptIds)) {
                deptIds = ListUtil.list(false, main.getId());
            } else {
                deptIds.add(main.getId());
            }
        }

        Set<Long> temp = new HashSet<>(deptIds);
        sysDeptService.setDepts(userId, temp.stream().toList());
    }
}
