package com.ingot.cloud.pms.service.domain.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.DeptConvert;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithMemberCountDTO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.mapper.TenantDeptMapper;
import com.ingot.cloud.pms.service.domain.TenantDeptService;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
@RequiredArgsConstructor
public class TenantDeptServiceImpl extends BaseServiceImpl<TenantDeptMapper, TenantDept> implements TenantDeptService {

    private final DeptConvert deptConvert;
    private final AssertionChecker assertionChecker;

    @Override
    public List<DeptTreeNodeVO> treeList() {
        List<TenantDept> all = list();
        List<DeptTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(TenantDept::getSort))
                .map(deptConvert::to).collect(Collectors.toList());

        return TreeUtil.build(allNode, IDConstants.ROOT_TREE_ID);
    }

    @Override
    public List<DeptTreeNodeVO> treeList(TenantDept condition) {
        List<DeptTreeNodeVO> nodeList = list().stream()
                .filter(BizFilter.deptFilter(condition))
                .sorted(Comparator.comparingInt(TenantDept::getSort))
                .map(deptConvert::to).collect(Collectors.toList());

        List<DeptTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    @Override
    public List<DeptWithMemberCountDTO> listWithMemberCount() {
        return baseMapper.listWithMemberCount();
    }

    @Override
    public void create(TenantDept params) {
        params.setCreatedAt(DateUtil.now());
        if (params.getStatus() == null) {
            params.setStatus(CommonStatusEnum.ENABLE);
        }
        save(params);
    }

    @Override
    public void update(TenantDept params) {
        params.setUpdatedAt(DateUtil.now());
        updateById(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        TenantDept dept = getById(id);
        assertionChecker.checkOperation(dept != null, "TenantDeptServiceImpl.NonExist");

        boolean success = count(Wrappers.<TenantDept>lambdaQuery().eq(TenantDept::getPid, id)) == 0;
        assertionChecker.checkOperation(success, "TenantDeptServiceImpl.ExistLeaf");

        removeById(id);
    }


    @Override
    public TenantDept getMainDept() {
        return getOne(Wrappers.<TenantDept>lambdaQuery()
                .eq(TenantDept::getMainFlag, Boolean.TRUE));
    }

    @Override
    public void clearByTenantId(long tenantId) {
        remove(Wrappers.<TenantDept>lambdaQuery()
                .eq(TenantDept::getTenantId, tenantId));
    }
}
