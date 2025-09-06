package com.ingot.cloud.pms.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysApplicationTenant;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.core.org.TenantUtils;
import com.ingot.cloud.pms.service.domain.SysApplicationTenantService;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>Description  : AuthorityUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/12/2.</p>
 * <p>Time         : 10:39.</p>
 */
public class AuthorityUtils {

    /**
     * 获取指定组织权限
     *
     * @param orgId                       组织ID
     * @param sysApplicationTenantService {@link SysApplicationTenantService}
     * @param sysAuthorityService         {@link SysAuthorityService}
     * @param authorityConvert              转换器
     * @return {@link AuthorityTreeNodeVO}
     */
    public static List<AuthorityTreeNodeVO> getOrgAuthorities(long orgId,
                                                              SysApplicationTenantService sysApplicationTenantService,
                                                              SysAuthorityService sysAuthorityService,
                                                              AuthorityConvert authorityConvert) {
        List<SysApplicationTenant> appList = sysApplicationTenantService.list(Wrappers.<SysApplicationTenant>lambdaQuery()
                .eq(SysApplicationTenant::getStatus, CommonStatusEnum.ENABLE));
        if (CollUtil.isEmpty(appList)) {
            return ListUtil.empty();
        }

        return appList.stream()
                .flatMap(app ->
                        TenantUtils.getTargetAuthorities(
                                        orgId, app.getAuthorityId(), sysAuthorityService, authorityConvert)
                                .stream())
                .toList();
    }

    /**
     * 过滤租户应用权限, 把权限列表中匹配到不可以用应用的权限去掉。<br>
     * 比如authorities中包含a权限，a权限对应的应用在appList中，并且该应用不可用，那么把a权限从authorities中去掉<br>
     * 去掉的权限编码比如是a.c，那么a.c.**的权限都需要去掉<br>
     */
    public static List<SysAuthority> filterOrgLockAuthority(List<SysAuthority> authorities,
                                                            SysApplicationTenantService sysApplicationTenantService) {
        List<SysApplicationTenant> appList = CollUtil.emptyIfNull(sysApplicationTenantService.list(
                Wrappers.<SysApplicationTenant>lambdaQuery().eq(SysApplicationTenant::getStatus, CommonStatusEnum.LOCK)));
        List<SysAuthority> removeAuthorities = appList.stream()
                .filter(app -> authorities.stream()
                        .anyMatch(auth -> Objects.equals(auth.getId(), app.getAuthorityId())))
                .map(app -> authorities.stream()
                        .filter(auth -> Objects.equals(auth.getId(), app.getAuthorityId()))
                        .findFirst().orElse(null))
                .toList();

        return authorities.stream()
                .filter(item -> removeAuthorities.stream()
                        .noneMatch(remove ->
                                Objects.equals(remove.getId(), item.getId())
                                        || StrUtil.startWith(item.getCode(), remove.getCode())))
                .toList();
    }

    /**
     * 权限转为tree结构
     *
     * @param authorities    {@link SysAuthority}
     * @param condition      条件
     * @param authorityConvert 转换器
     * @return {@link AuthorityTreeNodeVO}
     */
    public static List<AuthorityTreeNodeVO> mapTree(List<SysAuthority> authorities,
                                                    AuthorityFilterDTO condition,
                                                    AuthorityConvert authorityConvert) {
        List<AuthorityTreeNodeVO> nodeList = authorities.stream()
                .filter(BizFilter.authorityFilter(condition))
                .map(authorityConvert::to).collect(Collectors.toList());

        List<AuthorityTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

}
