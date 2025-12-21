package com.ingot.cloud.auth.service.biz;

import java.util.List;

import com.ingot.cloud.auth.model.dto.UserTokenQueryDTO;
import com.ingot.cloud.auth.model.vo.UserTokenVO;

/**
 * <p>Description  : BizUserTokenService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/21.</p>
 * <p>Time         : 13:30.</p>
 */
public interface BizUserTokenService {

    /**
     * 用户Token分页
     *
     * @param params {@link UserTokenQueryDTO}
     * @return {@link UserTokenVO}
     */
    List<UserTokenVO> userTokenPage(UserTokenQueryDTO params);
}
