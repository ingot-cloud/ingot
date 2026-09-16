package com.ingot.cloud.security.model.convert;

import java.util.List;

import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.cloud.security.api.model.vo.session.PlatformSessionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * <p>Auth Inner 会话出参到安全中心管理面出参的转换。</p>
 *
 * <p>会话侧只有登录账号名（{@code principalName}），昵称、头像、租户名不在会话主数据里，
 * 由 {@code SessionAdminService} 调 IAM 补全，因此这里只映射会话事实字段。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface PlatformSessionConvert {

    @Mapping(target = "username", source = "principalName")
    @Mapping(target = "nickname", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "tenantName", ignore = true)
    PlatformSessionVO to(InnerSessionVO in);

    List<PlatformSessionVO> to(List<InnerSessionVO> in);
}
