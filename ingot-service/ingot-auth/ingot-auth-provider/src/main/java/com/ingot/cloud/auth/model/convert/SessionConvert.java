package com.ingot.cloud.auth.model.convert;

import java.util.List;

import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import org.mapstruct.Mapper;

/**
 * <p>会话主数据到 Inner 契约出参的转换。</p>
 *
 * <p>{@link OnlineToken} 中的 authorities、deptIds、attributes 不属于 Inner 契约，
 * 由 MapStruct 按目标字段映射自然裁掉，避免鉴权数据随查询接口外泄。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface SessionConvert {

    InnerSessionVO to(OnlineToken in);

    List<InnerSessionVO> to(List<OnlineToken> in);
}
