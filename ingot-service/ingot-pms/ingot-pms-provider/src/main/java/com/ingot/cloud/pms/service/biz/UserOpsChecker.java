package com.ingot.cloud.pms.service.biz;

/**
 * <p>Description  : UserOpsChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/21.</p>
 * <p>Time         : 7:07 PM.</p>
 */
public interface UserOpsChecker {

    /**
     * 删除用户操作，不可删除自己，至少保留一个
     *
     * @param id 用户ID
     */
    void removeUser(long id);

    /**
     * 禁用用户操作
     *
     * @param id 用户ID
     */
    void disableUser(long id);
}
