package com.ingot.cloud.iam.support;

import java.util.Collection;

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
        return admit(domain, action).actor();
    }

    /**
     * 限定接口管理域、确认 ACTION 并给出治理资格来源。
     *
     * @param domain 服务器声明的管理域
     * @param action 本接口精确 ACTION
     * @return 当前成员与准入结论
     */
    public IamAdmission admit(AuthorizationDomain domain, IamAction action) {
        ActiveIdentity actor = current.requireDomain(domain);
        return new IamAdmission(actor, authorizer.admit(actor.context(), action).governed());
    }

    /**
     * 确认独立治理资格，委派入口及委派派生操作均不满足。
     * @param domain 管理域
     * @param action 精确操作
     * @return 可信身份
     */
    public ActiveIdentity requireGoverned(AuthorizationDomain domain, IamAction action) {
        IamAdmission admission = admit(domain, action);
        if (!admission.governed()) {
            throw new com.ingot.framework.commons.error.BizException(
                    com.ingot.framework.commons.model.iam.IamReasonCode.ACTION_DENIED);
        }
        return admission.actor();
    }

    /**
     * 查询精确操作资格，权限拒绝返回 false，依赖不可用仍传播。
     * @param domain 管理域
     * @param action 精确操作
     * @param direct 是否要求非委派可信来源
     * @return 当前是否具备资格
     */
    public boolean allows(AuthorizationDomain domain, IamAction action, boolean direct) {
        try {
            IamAdmission admission = admit(domain, action);
            return !direct || admission.governed();
        } catch (com.ingot.framework.commons.error.BizException exception) {
            if (com.ingot.framework.commons.model.iam.IamReasonCode.ACTION_DENIED.getCode()
                    .equals(exception.getCode())) {
                return false;
            }
            throw exception;
        }
    }

    /**
     * 为同一次只读查询批量取得展示资格，复用已恢复的身份，不逐操作重复查询身份。
     *
     * @param actor 本次请求通过 require、admit 或 requireCurrent 恢复的可信身份
     * @param actions 本次需要展示的精确操作
     * @return 仅限本次查询复用的能力；不得用于实际写入准入
     */
    public IamCapabilities capabilities(ActiveIdentity actor, Collection<IamAction> actions) {
        return new IamCapabilities(authorizer.capabilities(actor.context(), actions));
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
