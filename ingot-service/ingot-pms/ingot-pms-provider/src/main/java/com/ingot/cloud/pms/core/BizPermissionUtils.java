package com.ingot.cloud.pms.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.vo.permission.BizPermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.permission.BizPermissionVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.domain.MetaPermissionService;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.tenant.TenantEnv;

/**
 * <p>Description  : BizAuthorityUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/12/2.</p>
 * <p>Time         : 10:39.</p>
 */
public class BizPermissionUtils {

    /**
     * 获取指定组织权限
     *
     * @param orgId            组织ID
     * @param appService       {@link BizAppService}
     * @param authorityService {@link MetaPermissionService}
     * @param authorityConvert 转换器
     * @return {@link PermissionTreeNodeVO}
     */
    public static List<PermissionTreeNodeVO> getTenantAuthorities(long orgId,
                                                                  BizAppService appService,
                                                                  MetaPermissionService authorityService,
                                                                  AuthorityConvert authorityConvert) {
        List<MetaApp> appList = appService.getEnabledApps();
        if (CollUtil.isEmpty(appList)) {
            return ListUtil.empty();
        }

        return appList.stream()
                .flatMap(app ->
                        getTargetAuthorities(
                                orgId, app.getPermissionId(), authorityService, authorityConvert)
                                .stream())
                .toList();
    }

    /**
     * 获取指定组织的指定权限的所有子权限，包含指定权限
     *
     * @param orgId       组织ID
     * @param permissionId 权限ID
     * @param service     服务
     * @return 权限列表
     */
    public static List<PermissionTreeNodeVO> getTargetAuthorities(long orgId,
                                                                  long permissionId,
                                                                  MetaPermissionService service,
                                                                  AuthorityConvert authorityConvert) {
        return TenantEnv.applyAs(orgId, () -> {
            List<PermissionTreeNodeVO> list = new ArrayList<>();

            PermissionType authority = service.getById(permissionId);
            list.add(authorityConvert.toTreeNode(authority));

            List<MetaPermission> children = service.list(Wrappers.<MetaPermission>lambdaQuery()
                    .eq(MetaPermission::getPid, authority.getId()));
            if (CollUtil.isNotEmpty(children)) {
                children.forEach(itemMenu ->
                        list.addAll(getTargetAuthorities(orgId, itemMenu.getId(), service, authorityConvert)));
            }

            return list;
        });
    }

    /**
     * 过滤租户应用权限, 把权限列表中匹配到不可以用应用的权限去掉。<br>
     * 比如authorities中包含a权限，a权限对应的应用在appList中，并且该应用不可用，那么把a权限从authorities中去掉<br>
     * 去掉的权限编码比如是a.c，那么a.c.**的权限都需要去掉<br>
     */
    public static <T extends PermissionType> List<T> filterOrgLockAuthority(List<T> authorities,
                                                                            BizAppService appService) {
        List<MetaApp> appList = CollUtil.emptyIfNull(appService.getDisabledApps());

        List<PermissionType> removeAuthorities = appList.stream()
                .filter(app -> authorities.stream()
                        .anyMatch(auth -> Objects.equals(auth.getId(), app.getPermissionId())))
                .map(app -> authorities.stream()
                        .filter(auth -> Objects.equals(auth.getId(), app.getPermissionId()))
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
     * @param authorities      {@link PermissionType}
     * @param condition        条件
     * @param authorityConvert 转换器
     * @return {@link PermissionTreeNodeVO}
     */
    public static List<PermissionTreeNodeVO> mapTree(List<? extends PermissionType> authorities,
                                                     AuthorityConvert authorityConvert,
                                                     PermissionType condition) {
        List<PermissionTreeNodeVO> nodeList = authorities.stream()
                .filter(BizFilter.authorityFilter(condition))
                .map(authorityConvert::toTreeNode).collect(Collectors.toList());

        List<PermissionTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    /**
     * 权限转为tree结构
     *
     * @param authorities      {@link BizPermissionVO}
     * @param condition        条件
     * @param authorityConvert 转换器
     * @return {@link BizPermissionTreeNodeVO}
     */
    public static List<BizPermissionTreeNodeVO> bizMapTree(List<BizPermissionVO> authorities,
                                                           AuthorityConvert authorityConvert,
                                                           PermissionType condition) {
        List<BizPermissionTreeNodeVO> nodeList = authorities.stream()
                .filter(BizFilter.authorityFilter(condition))
                .map(authorityConvert::to).collect(Collectors.toList());

        List<BizPermissionTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    /**
     * 填充子权限
     */
    public static void fillChildren(List<PermissionType> result, List<? extends PermissionType> all, PermissionType parent) {
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
