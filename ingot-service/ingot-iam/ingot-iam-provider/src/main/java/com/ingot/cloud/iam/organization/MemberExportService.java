package com.ingot.cloud.iam.organization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.evaluation.ResourceScopeFilter;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.VersionInput;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>异步登记成员导出任务，生成已投影快照；下载时再次校验操作、范围和字段策略。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class MemberExportService {
    private static final String TARGET = "member-export";
    private static final TypeReference<Map<String, Object>> TASK = new TypeReference<>() {
    };
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final MemberQueryService members;
    private final IamAuditWriter audits;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final Executor executor;
    private final Path directory;

    /**
     * 绑定身份、范围与任务目录。
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param members 成员查询
     * @param audits 同事务审计
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     * @param executors 应用异步执行器；排除调度器，测试可空并同步执行
     */
    @Autowired
    public MemberExportService(IamAccess access, ResourceAccess scopes, MemberQueryService members,
                               IamAuditWriter audits, DataSource dataSource,
                               PlatformTransactionManager transactionManager,
                               ObjectProvider<TaskExecutor> executors) {
        this(access, scopes, members, audits, dataSource, transactionManager,
                applicationExecutor(executors),
                Path.of(System.getProperty("java.io.tmpdir"), "ingot-iam-member-export"));
    }

    /**
     * 指定任务目录，供测试隔离文件。
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param members 成员查询
     * @param audits 同事务审计
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     * @param executor 任务执行器
     * @param directory 任务目录
     */
    public MemberExportService(IamAccess access, ResourceAccess scopes, MemberQueryService members,
                               IamAuditWriter audits, DataSource dataSource,
                               PlatformTransactionManager transactionManager, Executor executor, Path directory) {
        this.access = access;
        this.scopes = scopes;
        this.members = members;
        this.audits = audits;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
        this.executor = executor == null ? Runnable::run : executor;
        this.directory = directory;
    }

    private static TaskExecutor applicationExecutor(ObjectProvider<TaskExecutor> executors) {
        if (executors == null) {
            return null;
        }
        return executors.orderedStream()
                .filter(candidate -> !(candidate instanceof TaskScheduler))
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验组织版本后登记导出任务，不把联系方式原值写入审计。
     *
     * @param input 组织版本
     * @return 导出任务 ID
     */
    public CreatedResource create(VersionInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
        long tenantId = IamIds.require(actor.context().tenantId());
        return transaction.execute(status -> {
            String version = tenantVersion(tenantId);
            IamIds.requireVersion(input.expectedVersion(), version);
            long exportId = access.nextId();
            writeTask(exportId, tenantId, IamIds.require(actor.context().memberId()), version);
            audits.write(actor.context(), exportId, TARGET, IamIds.text(tenantId), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, TARGET), Map.of("tenant", version));
            executor.execute(() -> snapshot(actor, exportId));
            return new CreatedResource(IamIds.text(exportId), version);
        });
    }

    /**
     * 再次校验导出操作与范围后返回当前字段策略投影，任务文件不能绕过权限。
     *
     * @param exportId 任务 ID
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> download(String exportId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
        long tenantId = IamIds.require(actor.context().tenantId());
        TaskFile task = readTask(IamIds.require(exportId));
        if (task == null || task.tenantId() != tenantId) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        ResourceScopeFilter.Predicate scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_EXPORT, "id");
        if ("(1=0)".equals(scope.sql())) {
            return IamPages.details(List.of(), 0, IamPages.DEFAULT_PAGE, IamPages.MAX_SIZE);
        }
        return members.listProjected(actor, IamAction.TENANT_MEMBER_EXPORT, IamPages.DEFAULT_PAGE, IamPages.MAX_SIZE,
                null, null);
    }

    private void snapshot(ActiveIdentity actor, long exportId) {
        try {
            PageResponse<ResourceDetail<MemberRecord>> page = members.listProjected(actor,
                    IamAction.TENANT_MEMBER_EXPORT, IamPages.DEFAULT_PAGE, IamPages.MAX_SIZE, null, null);
            Files.writeString(directory.resolve(exportId + ".json"), IamJson.object(page.items()),
                    StandardCharsets.UTF_8);
        } catch (RuntimeException | IOException exception) {
            try {
                Files.writeString(directory.resolve(exportId + ".failed"), "failed", StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // 下载走实时投影，快照失败不扩大权限。
            }
        }
    }

    private void writeTask(long exportId, long tenantId, long actorMemberId, String version) {
        try {
            Files.createDirectories(directory);
            Map<String, Object> task = new HashMap<>();
            task.put("id", IamIds.text(exportId));
            task.put("tenantId", tenantId);
            task.put("actorMemberId", actorMemberId);
            task.put("tenantVersion", version);
            Files.writeString(directory.resolve(exportId + ".task.json"), IamJson.object(task), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
    }

    private TaskFile readTask(long exportId) {
        Path path = directory.resolve(exportId + ".task.json");
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            Map<String, Object> document = IamJson.read(Files.readString(path, StandardCharsets.UTF_8), TASK);
            if (document == null || document.get("tenantId") == null) {
                return null;
            }
            return new TaskFile(Long.parseLong(document.get("tenantId").toString()));
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private String tenantVersion(long tenantId) {
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM iam_tenant WHERE id=:id AND deleted_at IS NULL",
                Map.of("id", tenantId), String.class);
        if (versions.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return versions.getFirst();
    }

    private record TaskFile(long tenantId) {
    }
}
