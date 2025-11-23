package com.ingot.cloud.pms.core;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.authority.BizAuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.authority.BizAuthorityVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.core.org.TenantUtils;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.domain.MetaAuthorityService;
import com.ingot.framework.commons.utils.tree.TreeUtil;

/**
 * <p>Description  : BizAuthorityUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/12/2.</p>
 * <p>Time         : 10:39.</p>
 */
public class BizAuthorityUtils {

    /**
     * 获取指定组织权限
     *
     * @param orgId            组织ID
     * @param appService       {@link BizAppService}
     * @param authorityService {@link MetaAuthorityService}
     * @param authorityConvert 转换器
     * @return {@link AuthorityTreeNodeVO}
     */
    public static List<AuthorityTreeNodeVO> getTenantAuthorities(long orgId,
                                                                 BizAppService appService,
                                                                 MetaAuthorityService authorityService,
                                                                 AuthorityConvert authorityConvert) {
        List<MetaApp> appList = appService.getEnabledApps();
        if (CollUtil.isEmpty(appList)) {
            return ListUtil.empty();
        }

        return appList.stream()
                .flatMap(app ->
                        TenantUtils.getTargetAuthorities(
                                        orgId, app.getAuthorityId(), authorityService, authorityConvert)
                                .stream())
                .toList();
    }

    /**
     * 过滤租户应用权限, 把权限列表中匹配到不可以用应用的权限去掉。<br>
     * 比如authorities中包含a权限，a权限对应的应用在appList中，并且该应用不可用，那么把a权限从authorities中去掉<br>
     * 去掉的权限编码比如是a.c，那么a.c.**的权限都需要去掉<br>
     */
    public static <T extends AuthorityType> List<T> filterOrgLockAuthority(List<T> authorities,
                                                                           BizAppService appService) {
        List<MetaApp> appList = CollUtil.emptyIfNull(appService.getDisabledApps());

        List<AuthorityType> removeAuthorities = appList.stream()
                .filter(app -> authorities.stream()
                        .anyMatch(auth -> Objects.equals(auth.getId(), app.getAuthorityId())))
                .map(app -> authorities.stream()
                        .filter(auth -> Objects.equals(auth.getId(), app.getAuthorityId()))
                        .findFirst().orElse(null))
                .collect(Collectors.toList());

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
     * @param authorities      {@link AuthorityType}
     * @param condition        条件
     * @param authorityConvert 转换器
     * @return {@link AuthorityTreeNodeVO}
     */
    public static List<AuthorityTreeNodeVO> mapTree(List<? extends AuthorityType> authorities,
                                                    AuthorityConvert authorityConvert,
                                                    AuthorityType condition) {
        List<AuthorityTreeNodeVO> nodeList = authorities.stream()
                .filter(BizFilter.authorityFilter(condition))
                .map(authorityConvert::to).collect(Collectors.toList());

        List<AuthorityTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    /**
     * 权限转为tree结构
     *
     * @param authorities      {@link BizAuthorityVO}
     * @param condition        条件
     * @param authorityConvert 转换器
     * @return {@link BizAuthorityTreeNodeVO}
     */
    public static List<BizAuthorityTreeNodeVO> bizMapTree(List<BizAuthorityVO> authorities,
                                                          AuthorityConvert authorityConvert,
                                                          AuthorityType condition) {
        List<BizAuthorityTreeNodeVO> nodeList = authorities.stream()
                .filter(BizFilter.authorityFilter(condition))
                .map(authorityConvert::to).collect(Collectors.toList());

        List<BizAuthorityTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    /**
     * 填充子权限
     */
    public static void fillChildren(List<AuthorityType> result, List<? extends AuthorityType> all, AuthorityType parent) {
        all.stream()
                .filter(item -> item.getPid() != null && item.getPid().equals(parent.getId()))
                .forEach(item -> {
                    if (result.stream().noneMatch(a -> Objects.equals(a.getId(), item.getId()))) {
                        result.add(item);
                    }
                    fillChildren(result, all, item);
                });
    }
}
