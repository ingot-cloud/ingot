package com.ingot.cloud.iam.support;

import lombok.RequiredArgsConstructor;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.identity.InitializationIdAllocator;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import org.springframework.stereotype.Service;

/**
 * <p>在管理命令入口恢复可信身份并校验精确 ACTION，不接受请求头中的替代域。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class IamAccess {
    private final CurrentIdentityService current;
    private final IamActionAuthorizer authorizer;
    private final InitializationIdAllocator ids;



    /**
     * 限定接口管理域并确认 ACTION。
     *
     * @param domain 服务器声明的管理域
     * @param action 本接口精确 ACTION
     * @return 状态有效的当前成员
     */
    public ActiveIdentity require(AuthorizationDomain domain, IamAction action) {
        ActiveIdentity actor = current.requireDomain(domain);
        authorizer.require(actor.context(), action);
        return actor;
    }

    /**
     * 只恢复当前身份，用于无独立 ACTION 的会话视图。
     *
     * @return 当前有效成员
     */
    public ActiveIdentity requireCurrent() {
        return current.requireCurrent();
    }

    /**
     * 分配下一个正数标识。
     *
     * @return 发号结果
     */
    public long nextId() {
        return ids.nextId();
    }
}
