package com.ingot.cloud.iam.support;

import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.IamRoleRevisionJoin;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>平台系统超管管理的事务锁及最后可用身份保护；调用方必须持有平台授权锁。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformAdministratorGuard {
    private final AssignmentRepository assignments;
    private final com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier changes;

    /** 首先取得平台授权串行锁，之后才锁成员、账号或分配。 */
    public void lock() { assignments.lockAuthorization(AuthorizationDomain.PLATFORM); }

    /** 当前事务提交后失效相关节点的授权缓存。 */
    public void changed() { changes.markAll(); }

    /** 当前事务写入后必须保留长期有效且可用的超管，失败由外层事务全部回滚。 */
    public void requireAvailable() { assignments.requireAvailableAdministrator(); changes.markAll(); }

    /**
     * 用服务端定义识别系统角色，普通角色和租户定义不能获得超管资格。
     * @param revision 真实版本和定义联查
     * @return 是否为平台内置超管
     */
    public static boolean isAdministrator(IamRoleRevisionJoin revision) {
        return revision != null && revision.getKind() == RoleKind.SYSTEM
                && revision.getDomain() == AuthorizationDomain.PLATFORM && revision.getTenantId() == null
                && RoleConstants.ROLE_ADMIN_CODE.equals(revision.getCode());
    }
}
