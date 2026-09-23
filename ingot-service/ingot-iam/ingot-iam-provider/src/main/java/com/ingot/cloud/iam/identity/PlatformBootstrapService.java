package com.ingot.cloud.iam.identity;

import java.util.List;
import java.util.Optional;

import com.ingot.cloud.iam.persistence.InitializationCatalogRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.PlatformBootstrapRepository;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>在新环境建立首个受控平台身份：平台账号、平台成员与系统治理授权。</p>
 *
 * <p>目录、治理角色与默认策略由冷启动种子写入，此处只补齐种子无法承载的凭证部分。
 * 账号一律经安全框架的注册用例创建，由该用例负责凭证策略、初始改密标记、锁定态初始化、
 * 密码历史与账号创建事件；本服务不直接写凭证，也不重写口令策略。</p>
 *
 * <p>整体幂等：已存在任意平台成员即视为身份已建立并跳过；登录名已被占用时复用该账号
 * 而不是重复创建。执行前提是种子已提供平台域唯一的系统治理版本，缺失时立即失败，
 * 避免建立一个没有任何治理能力的平台账号。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see IamBootstrapConfiguration
 */
@Slf4j
@RequiredArgsConstructor
public class PlatformBootstrapService {
    private final IamBootstrapProperties properties;
    private final PlatformBootstrapRepository bootstrap;
    private final InitializationCatalogRepository catalog;
    private final MemberQueryRepository members;
    private final InitializationIdAllocator ids;
    private final RegisterUserUseCase registerUser;
    private final InitialPasswordService initialPassword;

    /**
     * 建立首个平台身份，重复执行不改动既有数据。
     *
     * @throws IllegalStateException 冷启动种子缺少平台域唯一的系统治理版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void initialize() {
        if (bootstrap.anyPlatformMember()) {
            log.info("[IamBootstrap] 平台身份已存在，跳过冷启动");
            return;
        }
        long revisionId = governanceRevision();
        long accountId = account();
        long memberId = ids.nextId();
        members.insertPlatform(memberId, accountId, properties.getDisplayName(), null);
        bootstrap.insertGovernanceAssignment(ids.nextId(), memberId, revisionId);
        log.info("[IamBootstrap] 平台身份已建立 username={} accountId={} memberId={} revisionId={}",
                properties.getUsername(), accountId, memberId, revisionId);
    }

    private long governanceRevision() {
        List<Long> roles = catalog.systemRoles(AuthorizationDomain.PLATFORM);
        if (roles.size() != 1) {
            throw new IllegalStateException(
                    "冷启动要求平台域恰好一个启用的系统治理角色，当前为 " + roles.size()
                            + "，请先执行 databases/iam/006_bootstrap.sql");
        }
        List<Long> revisions = catalog.systemRevisions(roles.getFirst());
        if (revisions.isEmpty()) {
            throw new IllegalStateException(
                    "平台域系统治理角色缺少已发布版本，请先执行 databases/iam/006_bootstrap.sql");
        }
        return revisions.getFirst();
    }

    private long account() {
        Optional<Long> existing = bootstrap.findAccountByUsername(properties.getUsername());
        if (existing.isPresent()) {
            log.info("[IamBootstrap] 复用已存在账号 username={}", properties.getUsername());
            return existing.get();
        }
        // 初始口令由凭证框架的初始密码策略生成，按策略在有效期内首登强制改密并用后失效。
        String password = initialPassword.generate();
        Long accountId = registerUser.register(RegisterUserUseCase.RegisterUserCommand.builder()
                .creationSource(RegisterUserUseCase.CreationSource.ADMIN_CREATE)
                .userType(UserTypeEnum.ADMIN)
                .username(properties.getUsername())
                .password(password)
                .phone(properties.getPhone())
                .email(properties.getEmail())
                .nickname(properties.getDisplayName())
                .eventSource(EventSource.IAM)
                .build()).getId();
        // 冷启动没有可回传初始口令的调用方，运维只能从这一条日志取得口令。
        log.warn("[IamBootstrap] 平台账号 {} 初始口令为 {}，请立即登录并修改；口令按初始密码策略"
                + "限时有效且用后失效", properties.getUsername(), password);
        return accountId;
    }
}
