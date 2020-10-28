package com.ingot.framework.security.provider.service;

import com.ingot.framework.security.model.dto.UserTokenDto;

/**
 * <p>Description  : UserTokenService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/4.</p>
 * <p>Time         : 下午1:50.</p>
 */
public interface UserAccessTokenRedisService {

    /**
     * 更新用户token
     */
    void update(String oldToken, String newToken, int tokenValidateSeconds, UserTokenDto userTokenDto);

    /**
     * 更新用户 Unique Access Token
     * @param accessToken 访问 token
     * @param accessTokenValidateSeconds 失效时间
     * @param userTokenDto 保存数据
     */
    void updateUnique(String accessToken, int accessTokenValidateSeconds, UserTokenDto userTokenDto);

    /**
     * 更新用户 Standard Access Token
     * @param oldToken 当前token, 如果当前token不为空，那么把当前token revoke
     * @param newToken 更新的新token
     * @param tokenValidateSeconds 失效时间
     * @param userTokenDto 保存数据
     */
    void updateStandard(String oldToken, String newToken, int tokenValidateSeconds, UserTokenDto userTokenDto);

    /**
     * 删除用户 Access Token
     * @param accessToken Access Token
     */
    void revoke(String accessToken);

    /**
     * 根据 key 删除 token
     * @param key redis key
     */
    void revokeByKey(String key);

    /**
     * 根据用户id和用户名称删除 token
     */
    void revokeByUserInfo(String userId, String username);

    /**
     * 获取当前用户的 Token Dto
     * @param accessToken 访问 token
     * @return UserTokenDto
     */
    UserTokenDto getAccessToken(String accessToken);

    /**
     * Token 是否有效
     * @return Boolean
     */
    boolean isValid(String accessToken);
}
