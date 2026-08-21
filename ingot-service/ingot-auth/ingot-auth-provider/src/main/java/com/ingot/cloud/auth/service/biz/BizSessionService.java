package com.ingot.cloud.auth.service.biz;

import java.util.List;

import com.ingot.cloud.auth.api.model.dto.InnerSessionQueryDTO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionPageVO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;

/**
 * <p>在线会话查询编排，把 Redis 索引组合成 Inner 契约要求的分页与列表形态。</p>
 *
 * <p>会话存储层只提供「按 sid / 按用户 / 按 IP」的原子读取，分页游标选择、过滤条件组合与
 * 出参裁剪都在本层完成，存储层不感知查询语义。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService
 */
public interface BizSessionService {

    /**
     * 分页查询在线会话。
     *
     * @param params {@code tenantId} 与 {@code clientId} 必填；{@code userId} / {@code ipAddress} 可选
     */
    InnerSessionPageVO page(InnerSessionQueryDTO params);

    /**
     * 查询单个会话。
     *
     * @return 会话；已过期或已撤销时为 {@code null}
     */
    InnerSessionVO getBySid(String sid);

    /**
     * 查询用户名下的在线会话，按会话创建时间倒序。
     *
     * @param params {@code tenantId} 与 {@code userId} 必填；{@code clientId} 为空表示全部 Client
     */
    List<InnerSessionVO> listByUser(InnerSessionQueryDTO params);
}
