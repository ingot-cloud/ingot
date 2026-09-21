package com.ingot.cloud.iam.account;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.AccountCredentialRepository;
import com.ingot.cloud.iam.persistence.AccountQueryRepository;
import com.ingot.cloud.iam.persistence.AccountWriteRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AccountCreateInput;
import com.ingot.framework.commons.model.iam.AccountLockInput;
import com.ingot.framework.commons.model.iam.AccountLookupInput;
import com.ingot.framework.commons.model.iam.AccountLookupPurpose;
import com.ingot.framework.commons.model.iam.AccountRecord;
import com.ingot.framework.commons.model.iam.AccountSecret;
import com.ingot.framework.commons.model.iam.AccountUpdateInput;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ObjectCapability;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.VersionInput;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.model.enums.LockReason;
import com.ingot.framework.security.account.domain.port.inbound.ChangePasswordUseCase;
import com.ingot.framework.security.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.security.account.domain.port.inbound.ManageAccountStatusUseCase;
import com.ingot.framework.security.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.security.account.domain.port.inbound.UnlockAccountUseCase;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>提供平台全局账号查询与安全命令，复用框架注册/锁定/改密用例，不返回组织关系。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class AccountService {
    private static final List<IamAction> WRITE_ACTIONS = List.of(
            IamAction.PLATFORM_ACCOUNT_UPDATE, IamAction.PLATFORM_ACCOUNT_DELETE,
            IamAction.PLATFORM_ACCOUNT_ENABLE, IamAction.PLATFORM_ACCOUNT_DISABLE,
            IamAction.PLATFORM_ACCOUNT_LOCK, IamAction.PLATFORM_ACCOUNT_UNLOCK,
            IamAction.PLATFORM_ACCOUNT_RESET_PASSWORD);
    private static final String ALLOWED_MESSAGE = "当前对象允许该操作";
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final AuthorizationEvaluator evaluator;
    private final AccountQueryRepository accounts;
    private final AccountWriteRepository writes;
    private final AccountCredentialRepository credentials;
    private final MemberQueryRepository members;
    private final IamAuditWriter audits;
    private final RegisterUserUseCase registerUser;
    private final ManageAccountStatusUseCase statuses;
    private final LockAccountUseCase locks;
    private final UnlockAccountUseCase unlocks;
    private final ChangePasswordUseCase passwords;
    private final InitialPasswordService initialPasswords;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、范围、账号持久化与安全用例。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param evaluator 授权视图
     * @param accounts 账号查询
     * @param writes 账号写入
     * @param credentials 锁定事实
     * @param members 成员引用
     * @param audits 同事务审计
     * @param registerUser 注册用例
     * @param statuses 启停用例
     * @param locks 锁定用例
     * @param unlocks 解锁用例
     * @param passwords 密码用例
     * @param initialPasswords 初始密码
     * @param transactionManager 同一数据源事务
     */
    public AccountService(IamAccess access, ResourceAccess scopes, AuthorizationEvaluator evaluator,
                          AccountQueryRepository accounts, AccountWriteRepository writes,
                          AccountCredentialRepository credentials, MemberQueryRepository members,
                          IamAuditWriter audits, RegisterUserUseCase registerUser,
                          ManageAccountStatusUseCase statuses, LockAccountUseCase locks,
                          UnlockAccountUseCase unlocks, ChangePasswordUseCase passwords,
                          InitialPasswordService initialPasswords, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.scopes = scopes;
        this.evaluator = evaluator;
        this.accounts = accounts;
        this.writes = writes;
        this.credentials = credentials;
        this.members = members;
        this.audits = audits;
        this.registerUser = registerUser;
        this.statuses = statuses;
        this.locks = locks;
        this.unlocks = unlocks;
        this.passwords = passwords;
        this.initialPasswords = initialPasswords;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出全局账号。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 账号页
     */
    public PageResponse<ResourceDetail<AccountRecord>> list(int page, int pageSize) {
        IamPages.require(page, pageSize);
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_READ);
        ObjectScope scope = scopes.objects(actor.context(), IamAction.PLATFORM_ACCOUNT_READ);
        AuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(actor.context());
        var result = accounts.page(scope, page, pageSize);
        List<ResourceDetail<AccountRecord>> items = result.getRecords().stream()
                .map(row -> detail(actor, view, row))
                .toList();
        return IamPages.details(items, result.getTotal(), page, pageSize);
    }

    /**
     * 读取全局账号详情。
     *
     * @param id 账号 ID
     * @return 安全投影
     */
    public ResourceDetail<AccountRecord> get(String id) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_READ);
        long accountId = IamIds.require(id);
        scopes.requireVisibleObject(actor.context(), IamAction.PLATFORM_ACCOUNT_READ, accountId);
        IamAccountEntity row = accounts.find(accountId);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return detail(actor, evaluator.evaluate(actor.context()), row);
    }

    /**
     * 按用途精确查找一个账号，未命中按对象不存在处理。
     *
     * @param input 单一查找条件
     * @return 受限投影
     */
    public ResourceDetail<AccountRecord> lookup(AccountLookupInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_LOOKUP);
        IamAccountEntity row = resolveLookup(input);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        long accountId = row.getId().longValueExact();
        scopes.requireVisibleObject(actor.context(), IamAction.PLATFORM_ACCOUNT_LOOKUP, accountId);
        if (input.purpose() == AccountLookupPurpose.MEMBER_CREATE) {
            AccountRecord record = new AccountRecord(IamIds.text(accountId), row.getUsername(),
                    null, null, Boolean.TRUE.equals(row.getEnabled()), credentials.locked(accountId),
                    Boolean.TRUE.equals(row.getMustChangePassword()), null);
            return IamDetails.of(record, version(row));
        }
        return detail(actor, evaluator.evaluate(actor.context()), row);
    }

    /**
     * 创建全局账号，初始密码由安全框架生成，不自动授予成员资格。
     *
     * @param input 登录名与可选联系方式
     * @return 新账号 ID 与版本
     * @throws BizException 登录名已存在或注册用例拒绝时使用 InvalidArgument
     */
    public CreatedResource create(AccountCreateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_CREATE);
        if (accounts.findByUsername(input.username()) != null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "登录名已存在");
        }
        long operatorId = IamIds.require(actor.context().accountId());
        String password = initialPasswords.generate();
        try {
            registerUser.register(RegisterUserUseCase.RegisterUserCommand.builder()
                    .creationSource(RegisterUserUseCase.CreationSource.ADMIN_CREATE)
                    .userType(UserTypeEnum.ADMIN)
                    .username(input.username())
                    .password(password)
                    .phone(blankToNull(input.phone()))
                    .email(blankToNull(input.email()))
                    .createdBy(operatorId)
                    .eventSource(EventSource.IAM)
                    .build());
        } catch (IllegalArgumentException exception) {
            String detail = exception.getMessage();
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(),
                    detail == null || detail.isBlank() ? IamReasonCode.INVALID_ARGUMENT.getText() : detail);
        }
        IamAccountEntity row = accounts.findByUsername(input.username());
        if (row == null) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        transaction.executeWithoutResult(status -> audits.write(actor.context(), access.nextId(), "account",
                row.getId().toString(), AuditChangeType.CREATE, Map.of(),
                Map.of(AuditField.NAME, row.getUsername(), AuditField.STATUS, enabled(row)),
                Map.of("account", version(row))));
        return new CreatedResource(row.getId().toString(), version(row));
    }

    /**
     * 更新全局账号联系资料。
     *
     * @param id 账号 ID
     * @param input 联系资料
     * @return 更新后投影
     */
    public ResourceDetail<AccountRecord> update(String id, AccountUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_UPDATE);
        long accountId = IamIds.require(id);
        return transaction.execute(status -> {
            IamAccountEntity locked = requireLocked(actor, IamAction.PLATFORM_ACCOUNT_UPDATE, accountId,
                    input.expectedVersion());
            if (writes.updateContacts(accountId, input.phone(), input.email(),
                    locked.getVersion().longValueExact()) != 1) {
                throw new BizException(IamReasonCode.REVISION_CONFLICT);
            }
            IamAccountEntity next = accounts.find(accountId);
            audits.write(actor.context(), access.nextId(), "account", IamIds.text(accountId),
                    AuditChangeType.UPDATE, Map.of(AuditField.NAME, locked.getUsername()),
                    Map.of(AuditField.NAME, next.getUsername()), Map.of("account", version(next)));
            return detail(actor, evaluator.evaluate(actor.context()), next);
        });
    }

    /**
     * 删除未被成员引用的全局账号。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 删除后的版本信封
     */
    public CreatedResource delete(String id, VersionInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_DELETE);
        long accountId = IamIds.require(id);
        return transaction.execute(status -> {
            IamAccountEntity locked = requireLocked(actor, IamAction.PLATFORM_ACCOUNT_DELETE, accountId,
                    input.expectedVersion());
            if (members.hasMembership(accountId)) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            if (writes.delete(accountId) != 1) {
                throw new BizException(IamReasonCode.REVISION_CONFLICT);
            }
            audits.write(actor.context(), access.nextId(), "account", IamIds.text(accountId),
                    AuditChangeType.REMOVE, Map.of(AuditField.NAME, locked.getUsername()), Map.of(),
                    Map.of("account", version(locked)));
            return new CreatedResource(IamIds.text(accountId), version(locked));
        });
    }

    /**
     * 启用全局账号。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 版本信封
     */
    public CreatedResource enable(String id, VersionInput input) {
        return changeStatus(id, input, true, IamAction.PLATFORM_ACCOUNT_ENABLE, AuditChangeType.ENABLE);
    }

    /**
     * 停用全局账号。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 版本信封
     */
    public CreatedResource disable(String id, VersionInput input) {
        return changeStatus(id, input, false, IamAction.PLATFORM_ACCOUNT_DISABLE, AuditChangeType.DISABLE);
    }

    /**
     * 手动锁定全局账号。
     *
     * @param id 账号 ID
     * @param input 原因与期限
     * @return 版本信封
     */
    public CreatedResource lock(String id, AccountLockInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_LOCK);
        long accountId = IamIds.require(id);
        return transaction.execute(status -> {
            IamAccountEntity locked = requireLocked(actor, IamAction.PLATFORM_ACCOUNT_LOCK, accountId,
                    input.expectedVersion());
            locks.lockManually(LockAccountUseCase.LockCommand.builder()
                    .userId(accountId)
                    .userType(UserTypeEnum.ADMIN)
                    .reason(LockReason.MANUAL_LOCK)
                    .reasonDetail(input.reasonDetail())
                    .lockedUntil(input.lockedUntil() == null ? null
                            : LocalDateTime.ofInstant(input.lockedUntil(), ZoneOffset.UTC))
                    .operatorId(IamIds.require(actor.context().accountId()))
                    .operatorName(actor.context().memberId())
                    .source(EventSource.IAM)
                    .build());
            audits.write(actor.context(), access.nextId(), "account", IamIds.text(accountId),
                    AuditChangeType.UPDATE, Map.of(AuditField.STATUS, enabled(locked)),
                    Map.of(AuditField.STATUS, "LOCKED"), Map.of("account", version(locked)));
            return new CreatedResource(IamIds.text(accountId), version(locked));
        });
    }

    /**
     * 手动解锁全局账号。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 版本信封
     */
    public CreatedResource unlock(String id, VersionInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_UNLOCK);
        long accountId = IamIds.require(id);
        return transaction.execute(status -> {
            IamAccountEntity locked = requireLocked(actor, IamAction.PLATFORM_ACCOUNT_UNLOCK, accountId,
                    input.expectedVersion());
            unlocks.unlockManually(UnlockAccountUseCase.UnlockCommand.builder()
                    .userId(accountId)
                    .userType(UserTypeEnum.ADMIN)
                    .operatorId(IamIds.require(actor.context().accountId()))
                    .operatorName(actor.context().memberId())
                    .source(EventSource.IAM)
                    .build());
            audits.write(actor.context(), access.nextId(), "account", IamIds.text(accountId),
                    AuditChangeType.UPDATE, Map.of(AuditField.STATUS, "LOCKED"),
                    Map.of(AuditField.STATUS, enabled(locked)), Map.of("account", version(locked)));
            return new CreatedResource(IamIds.text(accountId), version(locked));
        });
    }

    /**
     * 重置全局账号密码并一次性返回明文初始密码。
     *
     * @param id 账号 ID
     * @param input 期望版本
     * @return 初始密码
     */
    public AccountSecret resetPassword(String id, VersionInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACCOUNT_RESET_PASSWORD);
        long accountId = IamIds.require(id);
        return transaction.execute(status -> {
            IamAccountEntity locked = requireLocked(actor, IamAction.PLATFORM_ACCOUNT_RESET_PASSWORD, accountId,
                    input.expectedVersion());
            String password = initialPasswords.generate();
            passwords.resetPassword(ChangePasswordUseCase.ResetPasswordCommand.builder()
                    .userId(accountId)
                    .userType(UserTypeEnum.ADMIN)
                    .newPassword(password)
                    .operatorId(IamIds.require(actor.context().accountId()))
                    .operatorName(actor.context().memberId())
                    .source(EventSource.IAM)
                    .build());
            audits.write(actor.context(), access.nextId(), "account", IamIds.text(accountId),
                    AuditChangeType.UPDATE, Map.of(AuditField.NAME, locked.getUsername()),
                    Map.of(AuditField.NAME, locked.getUsername()), Map.of("account", version(locked)));
            return new AccountSecret(password);
        });
    }

    private CreatedResource changeStatus(String id, VersionInput input, boolean enabled, IamAction action,
                                         AuditChangeType changeType) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, action);
        long accountId = IamIds.require(id);
        return transaction.execute(status -> {
            IamAccountEntity locked = requireLocked(actor, action, accountId, input.expectedVersion());
            ManageAccountStatusUseCase.StatusCommand command = ManageAccountStatusUseCase.StatusCommand.builder()
                    .userId(accountId)
                    .userType(UserTypeEnum.ADMIN)
                    .targetStatus(enabled)
                    .operatorId(IamIds.require(actor.context().accountId()))
                    .operatorName(actor.context().memberId())
                    .source(EventSource.IAM)
                    .build();
            if (enabled) {
                statuses.enableAccount(command);
            } else {
                statuses.disableAccount(command);
            }
            IamAccountEntity next = accounts.find(accountId);
            audits.write(actor.context(), access.nextId(), "account", IamIds.text(accountId), changeType,
                    Map.of(AuditField.STATUS, enabled(locked)), Map.of(AuditField.STATUS, enabled(next)),
                    Map.of("account", version(next)));
            return new CreatedResource(IamIds.text(accountId), version(next));
        });
    }

    private IamAccountEntity requireLocked(ActiveIdentity actor, IamAction action, long accountId,
                                           String expectedVersion) {
        scopes.requireVisibleObject(actor.context(), action, accountId);
        IamAccountEntity locked = accounts.lock(accountId);
        if (locked == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        IamIds.requireVersion(expectedVersion, version(locked));
        return locked;
    }

    private IamAccountEntity resolveLookup(AccountLookupInput input) {
        if (input.username() != null && !input.username().isBlank()) {
            return accounts.findByUsername(input.username());
        }
        if (input.phone() != null && !input.phone().isBlank()) {
            return accounts.findByPhone(input.phone());
        }
        return accounts.findByEmail(input.email());
    }

    private ResourceDetail<AccountRecord> detail(ActiveIdentity actor, AuthorizationEvaluator.AuthorizationView view,
                                                 IamAccountEntity row) {
        long accountId = row.getId().longValueExact();
        return IamDetails.of(record(row), capabilities(actor, view, accountId), version(row));
    }

    private AccountRecord record(IamAccountEntity row) {
        long accountId = row.getId().longValueExact();
        Instant lastLogin = row.getLastLoginAt() == null ? null : row.getLastLoginAt().toInstant(ZoneOffset.UTC);
        return new AccountRecord(IamIds.text(accountId), row.getUsername(), row.getPhone(), row.getEmail(),
                Boolean.TRUE.equals(row.getEnabled()), credentials.locked(accountId),
                Boolean.TRUE.equals(row.getMustChangePassword()), lastLogin);
    }

    private Map<String, ObjectCapability> capabilities(ActiveIdentity actor,
                                                       AuthorizationEvaluator.AuthorizationView view, long accountId) {
        Map<String, ObjectCapability> result = new LinkedHashMap<>();
        for (IamAction action : WRITE_ACTIONS) {
            if (!view.actionCodes().contains(action.getCode())) {
                result.put(action.getCode(), new ObjectCapability(false, IamReasonCode.ACTION_DENIED,
                        IamReasonCode.ACTION_DENIED.getText()));
                continue;
            }
            boolean allowed = ObjectScopeSql.matches(scopes.objects(actor.context(), action), accountId);
            result.put(action.getCode(), allowed
                    ? new ObjectCapability(true, null, ALLOWED_MESSAGE)
                    : new ObjectCapability(false, IamReasonCode.DATA_SCOPE_DENIED,
                    IamReasonCode.DATA_SCOPE_DENIED.getText()));
        }
        return result;
    }

    private static String version(IamAccountEntity row) {
        return row.getVersion() == null ? "0" : row.getVersion().toString();
    }

    private static String enabled(IamAccountEntity row) {
        return Boolean.TRUE.equals(row.getEnabled()) ? "ENABLED" : "DISABLED";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
