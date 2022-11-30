package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.service.biz.UserDetailService;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : UserDetailServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/29.</p>
 * <p>Time         : 5:27 下午.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailService {
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final Oauth2RegisteredClientService oauth2RegisteredClientService;
    private final Map<String, SocialProcessor> socialProcessorMap;
    private final UserTrans userTrans;

    @Override
    public UserDetailsResponse getUserAuthDetails(String username) {
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));

        UserDetailsResponse result = userTrans.toUserDetails(user);
        // 查询拥有的角色
        List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), user.getDeptId());
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode)
                .collect(Collectors.toList());
        result.setRoles(roleCodes);

        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        // 查询可访问的客户端
        List<String> clientIds = Optional.ofNullable(oauth2RegisteredClientService.getClientsByRoles(roleIds))
                .map(clients -> clients.stream()
                        .map(Oauth2RegisteredClient::getClientId).collect(Collectors.toSet()))
                .map(ListUtil::toList)
                .orElse(CollUtil.empty(List.class));
        result.setClients(clientIds);
        return result;
    }

//    private SysUser withSocialMode(UserDetailsRequest params) {
//        String[] extract = SocialUtils.extract(params.getUniqueCode());
//        SocialTypeEnum socialType = SocialTypeEnum.get(extract[0]);
//        if (socialType == null) {
//            OAuth2ErrorUtils.throwInvalidRequest("非法社交类型");
//        }
//
//        SocialProcessor processor = socialProcessorMap.get(socialType.getBeanName());
//        if (processor == null) {
//            OAuth2ErrorUtils.throwInvalidRequest("非法社交类型");
//        }
//
//        params.setUniqueCode(extract[1]);
//        return processor.exec(params);
//    }
}
