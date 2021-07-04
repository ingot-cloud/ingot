package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
import com.ingot.cloud.pms.api.model.transform.DeptTrans;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNode;
import com.ingot.cloud.pms.api.utils.TreeUtils;
import com.ingot.cloud.pms.mapper.SysDeptMapper;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.cloud.pms.service.domain.SysRoleDeptService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@AllArgsConstructor
public class SysDeptServiceImpl extends BaseServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {
    private final SysUserService sysUserService;
    private final SysRoleDeptService sysRoleDeptService;

    private final DeptTrans deptTrans;
    private final IdGenerator idGenerator;
    private final AssertI18nService assertI18nService;

    @Override
    public List<DeptTreeNode> tree() {
        List<SysDept> all = list();

        List<DeptTreeNode> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysDept::getSort))
                .map(deptTrans::to).collect(Collectors.toList());

        return TreeUtils.build(allNode, 0);
    }

    @Override
    public void createDept(SysDept params) {
        params.setCreatedAt(DateUtils.now());
        params.setId(idGenerator.nextId());
        if (params.getStatus() == null) {
            params.setStatus(CommonStatusEnum.ENABLE);
        }
        assertI18nService.checkOperation(save(params),
                "SysDeptServiceImpl.CreateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDeptById(long id) {
        int existLeaf = count(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getPid, id));
        assertI18nService.checkOperation(existLeaf == 0,
                "SysDeptServiceImpl.ExistLeaf");

        // 判断是关联了用户，关联用户则不可删除
        boolean hasUser = sysUserService.matchDept(id);
        assertI18nService.checkOperation(hasUser, "SysDeptServiceImpl.ExistUser");

        // 取消关联角色
        assertI18nService.checkOperation(sysRoleDeptService.remove(Wrappers.<SysRoleDept>lambdaQuery()
                .eq(SysRoleDept::getDeptId, id)), "SysDeptServiceImpl.RemoveFailed");

        assertI18nService.checkOperation(removeById(id),
                "SysDeptServiceImpl.RemoveFailed");
    }

    @Override
    public void updateDept(SysDept params) {
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysDeptServiceImpl.UpdateFailed");
    }
}
