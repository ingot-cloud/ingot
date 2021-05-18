package com.ingot.cloud.pms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.transform.DeptTrans;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNode;
import com.ingot.cloud.pms.api.utils.TreeUtils;
import com.ingot.cloud.pms.mapper.SysDeptMapper;
import com.ingot.cloud.pms.service.SysDeptService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.core.validation.service.I18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@AllArgsConstructor
public class SysDeptServiceImpl extends BaseServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {
    private final DeptTrans deptTrans;
    private final IdGenerator idGenerator;
    private final I18nService i18nService;

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
        if (params.getStatus() == null){
            params.setStatus(CommonStatusEnum.ENABLE);
        }
        AssertionUtils.checkOperation(save(params),
                i18nService.getMessage("SysDeptServiceImpl.CreateFailed"));
    }

    @Override
    public void removeDeptById(long id) {
        int existLeaf = count(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getPid, id));
        AssertionUtils.checkOperation(existLeaf == 0,
                i18nService.getMessage("SysDeptServiceImpl.ExistLeaf"));

        AssertionUtils.checkOperation(removeById(id),
                "SysDeptServiceImpl.RemoveFailed");
    }

    @Override
    public void updateDept(SysDept params) {
        params.setUpdatedAt(DateUtils.now());
        AssertionUtils.checkOperation(updateById(params),
                "SysDeptServiceImpl.UpdateFailed");
    }
}
