package com.ingot.cloud.iam.identity;

import java.util.Map;
import java.util.Objects;

import com.ingot.cloud.iam.persistence.TenantInitializationRepository;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>在同一事务中写入最小组织、所有者治理引用、基础开通和默认策略引用，并可靠保存审计事实。</p>
 *
 * <p>这是内部持久化流程，不暴露 HTTP。调用方必须先验证创建组织 ACTION，并从服务器目录生成计划；
 * 此处不复制账号凭证、角色定义、资源、菜单或默认策略条目，也不为普通成员批量生成授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class TenantInitializer {
    private static final String TENANT_RESOURCE = "tenant";
    private final TransactionTemplate transaction;
    private final ActiveIdentityService identities;
    private final TenantInitializationRepository repository;
    private final IamAuditWriter audits;

    /**
     * 绑定同一数据源事务、身份校验与组织初始化仓库。
     * @param transactionManager 管理 IAM 数据源的事务管理器；Lombok 无法表达 TransactionTemplate 装配
     * @param identities 实时身份校验
     * @param repository 行锁与写入
     * @param audits 同事务审计
     */
    public TenantInitializer(PlatformTransactionManager transactionManager, ActiveIdentityService identities,
                             TenantInitializationRepository repository, IamAuditWriter audits) {
        this.transaction = new TransactionTemplate(transactionManager);
        this.identities = identities;
        this.repository = repository;
        this.audits = audits;
    }

    /**
     * 原子建立组织及必要引用；任一检查、写入或审计失败均回滚。
     *
     * @param actor 已完成创建组织 ACTION 校验的平台身份，仍在事务中验证身份状态
     * @param plan 服务器构造的初始化计划，不接受客户端提供治理或默认版本
     * @return 新组织标识及初始版本
     * @throws BizException 身份、基础应用、治理角色或默认版本无效时拒绝
     * @throws org.springframework.dao.DataAccessException 写入失败时抛出并回滚，包括标识冲突
     */
    public CreatedResource initialize(AuthorizationContext actor, TenantInitializationPlan plan) {
        Objects.requireNonNull(plan, "初始化计划不能为空");
        if (actor == null || actor.domain() != AuthorizationDomain.PLATFORM) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return transaction.execute(status -> {
            identities.requireActive(actor);
            if (!repository.lockActiveAccount(plan.ownerAccountId())) {
                throw new BizException(IamReasonCode.IDENTITY_INVALID);
            }
            if (!repository.lockTenantSystemRevision(plan.governanceRevisionId())) {
                throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            }
            for (var app : plan.applications()) {
                if (!repository.lockTenantApplication(app.applicationId())) {
                    throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
                }
            }
            if (!repository.lockDefaultRevision(plan.directoryRevisionId(), DefaultPolicyKind.DIRECTORY)
                    || !repository.lockDefaultRevision(plan.fieldRevisionId(), DefaultPolicyKind.FIELD)) {
                throw new BizException(IamReasonCode.POLICY_CONFLICT);
            }
            String version = repository.insertOrganization(plan);
            audits.write(actor, plan.auditId(), TENANT_RESOURCE, Long.toString(plan.tenantId()),
                    AuditChangeType.CREATE, Map.of(),
                    Map.of(AuditField.NAME, plan.name(), AuditField.OWNER_MEMBER, Long.toString(plan.ownerMemberId())),
                    Map.of(InitialReference.GOVERNANCE.name(), Long.toString(plan.governanceRevisionId()),
                            InitialReference.DIRECTORY.name(), Long.toString(plan.directoryRevisionId()),
                            InitialReference.FIELD.name(), Long.toString(plan.fieldRevisionId())));
            return new CreatedResource(Long.toString(plan.tenantId()), version);
        });
    }

    /**
     * <p>区分初始化审计固定的三类基础引用。</p>
     * @author jy
     * @since 1.0.0
     */
    private enum InitialReference {
        GOVERNANCE, DIRECTORY, FIELD
    }
}
