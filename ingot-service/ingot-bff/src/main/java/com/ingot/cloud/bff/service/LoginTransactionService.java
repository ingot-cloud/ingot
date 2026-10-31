package com.ingot.cloud.bff.service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.cloud.bff.model.BffCleanup;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.BffRedisKeys;
import com.ingot.framework.commons.constants.BffRedisScripts;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.bff.BffLoginStage;
import com.ingot.framework.commons.model.bff.BffSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 按应用同槽保存绑定、事务和会话，所有身份推进通过版本 CAS。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class LoginTransactionService {

    private static final Pattern OPAQUE_ID = Pattern.compile(BffConstants.OPAQUE_ID_REGEX);

    private final StringRedisTemplate redis;

    private final ObjectMapper mapper;

    private final BffProperties properties;

    private final Clock clock;

    /** UTC 秒时钟，供状态机和补偿使用。 */
    public long now() {
        return clock.instant().getEpochSecond();
    }

    /** 校验外部不透明 ID，避免旧协议、键注入及无界 key。 */
    public static boolean validId(String id) {
        return id != null && OPAQUE_ID.matcher(id).matches();
    }

    /** 仅从当前应用读取绑定；不创建或轮换。 */
    public AuthBinding findBinding(String appId, String id) {
        return validId(id) ? decode(redis.opsForValue().get(BffRedisKeys.binding(appId, id)), AuthBinding.class) : null;
    }

    /** NX 建立服务端随机绑定，重复候选不能覆盖。 */
    public boolean createBinding(AuthBinding binding) {
        return Boolean.TRUE.equals(redis.opsForValue()
            .setIfAbsent(BffRedisKeys.binding(binding.getAppId(), binding.getBindingId()), encode(binding),
                    Duration.ofSeconds(properties.getTransactionTtl())));
    }

    /** 绑定剩余寿命；不存在返回零。 */
    public long bindingTtl(String appId, String id) {
        Long ttl = redis.getExpire(BffRedisKeys.binding(appId, id));
        return ttl == null ? 0 : Math.max(0, ttl);
    }

    /** 退出移除本绑定，不能继续恢复结果。 */
    public void deleteBinding(String appId, String id) {
        if (validId(id))
            redis.delete(BffRedisKeys.binding(appId, id));
    }

    /** 原子预留容量、保存新事务和延长绑定；不会作废其他事务。 */
    public void create(LoginTransaction tx) {
        long result = run(BffRedisScripts.CREATE_TRANSACTION,
                List.of(BffRedisKeys.binding(tx.getAppId(), tx.getBindingId()), txKey(tx), activeKey(tx)),
                Long.toString(now()), Integer.toString(properties.getMaxActiveTransactions()), encode(tx),
                Long.toString(properties.getTransactionTtl()), Long.toString(tx.getExpiresAt()), tx.getTransactionId());
        if (result == -1)
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        if (result == -2)
            throw new BffAuthException(BffErrorCode.LOGIN_CAPACITY);
        if (result != 1)
            throw new BffAuthException(BffErrorCode.TRANSACTION_CONFLICT);
    }

    /** 只读并验证归属与阶段期限；GET 不修改绑定或延长期限。 */
    public LoginTransaction require(String appId, String id, String bindingId) {
        if (!validId(id))
            throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
        LoginTransaction tx = decode(redis.opsForValue().get(BffRedisKeys.transaction(appId, id)),
                LoginTransaction.class);
        if (tx == null)
            throw new BffAuthException(BffErrorCode.TRANSACTION_EXPIRED);
        if (!appId.equals(tx.getAppId()) || !bindingId.equals(tx.getBindingId()))
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        long deadline = tx.getStage() == BffLoginStage.COMPLETED ? tx.getRecoveryExpiresAt() : deadline(tx);
        if (deadline <= now())
            throw new BffAuthException(BffErrorCode.TRANSACTION_EXPIRED);
        return tx;
    }

    /** 返回硬期限与 Auth 可用窗口的较早值。 */
    public long deadline(LoginTransaction tx) {
        return tx.getAuthenticationExpiresAt() > 0 ? Math.min(tx.getExpiresAt(), tx.getAuthenticationExpiresAt())
                : tx.getExpiresAt();
    }

    /** 认领一次认证，租约到期也不允许另一工作者重新调用 Auth。 */
    public void claim(LoginTransaction tx, BffLoginStage expected) {
        if (tx.getStage() == BffLoginStage.AUTHENTICATING || tx.getStage() == BffLoginStage.COMMITTING
                || tx.getStage() == BffLoginStage.UNKNOWN)
            throw new BffAuthException(BffErrorCode.TRANSACTION_BUSY);
        if (tx.getStage() != expected)
            throw new BffAuthException(BffErrorCode.TRANSACTION_CONFLICT);
        tx.setLeaseUntil(Math.min(deadline(tx), now() + properties.getExecutionLeaseSeconds()));
        transition(tx, BffLoginStage.AUTHENTICATING, null);
    }

    /** 条件推进；已获 sid 的提交前载荷同时进入可靠撤销队列。 */
    public void transition(LoginTransaction tx, BffLoginStage target, BffSession issued) {
        if ((target == BffLoginStage.SELECT_TENANT || target == BffLoginStage.COMMITTING) && deadline(tx) <= now())
            throw new BffAuthException(BffErrorCode.TRANSACTION_EXPIRED);
        long previous = tx.getVersion();
        BffLoginStage before = tx.getStage();
        tx.setVersion(previous + 1);
        tx.setStage(target);
        boolean terminal = target == BffLoginStage.FAILED || target == BffLoginStage.EXPIRED;
        boolean leaseRequired = target == BffLoginStage.SELECT_TENANT || target == BffLoginStage.COMMITTING;
        long result = run(BffRedisScripts.CAS_TRANSACTION,
                List.of(txKey(tx), activeKey(tx), BffRedisKeys.binding(tx.getAppId(), tx.getBindingId()),
                        BffRedisKeys.cleanup(tx.getAppId()), BffRedisKeys.cleanupDue(tx.getAppId())),
                Long.toString(previous), before.getValue(), Long.toString(now()), encode(tx),
                Long.toString(Math.max(1, tx.getExpiresAt() - now())), terminal ? "1" : "0",
                issued == null ? "" : encode(cleanupOf(issued, false)), issued == null ? "" : issued.getSessionId(),
                Long.toString(tx.getLeaseUntil()), leaseRequired ? "1" : "0", Long.toString(deadline(tx)));
        if (result != 1) {
            tx.setVersion(previous);
            tx.setStage(before);
            throw new BffAuthException(BffErrorCode.TRANSACTION_CONFLICT);
        }
    }

    /** 一次性提交正式会话，替换同绑定/当前 Cookie 的旧会话并原子登记清理。 */
    public void commit(LoginTransaction tx, BffSession session, String oldSessionId) {
        BffLoginStage before = tx.getStage();
        long previous = tx.getVersion();
        tx.setStage(BffLoginStage.COMPLETED);
        tx.setVersion(previous + 1);
        tx.setResultSessionId(session.getSessionId());
        tx.setRecoveryExpiresAt(session.getRecoveryExpiresAt());
        tx.setAuthCookie(null);
        tx.setAllows(null);
        tx.setCodeVerifier(null);
        tx.setState(null);
        long result = run(BffRedisScripts.COMMIT_SESSION,
                List.of(txKey(tx), BffRedisKeys.session(tx.getAppId(), session.getSessionId()),
                        BffRedisKeys.current(tx.getAppId(), tx.getBindingId()), activeKey(tx),
                        BffRedisKeys.binding(tx.getAppId(), tx.getBindingId()),
                        BffRedisKeys.delivered(tx.getAppId(), session.getSessionId()),
                        BffRedisKeys.cleanup(tx.getAppId()), BffRedisKeys.cleanupDue(tx.getAppId())),
                Long.toString(previous), before.getValue(), Long.toString(now()), encode(session),
                Long.toString(Math.max(1, session.getExpiresAt() - now())), session.getSessionId(), encode(tx),
                Long.toString(Math.max(1, tx.getRecoveryExpiresAt() - now())), BffRedisKeys.prefix(tx.getAppId()),
                validId(oldSessionId) ? oldSessionId : "", encode(cleanupOf(session, true)),
                Long.toString(tx.getRecoveryExpiresAt()));
        if (result != 1)
            throw new BffAuthException(BffErrorCode.TRANSACTION_CONFLICT);
    }

    /** 只读当前代次会话；不返回已过期且未交付的结果。 */
    public BffSession readSession(String appId, String sessionId) {
        if (!validId(sessionId))
            return null;
        String raw = redis.execute(new DefaultRedisScript<>(BffRedisScripts.READ_SESSION, String.class),
                List.of(BffRedisKeys.session(appId, sessionId)), appId, Long.toString(now()),
                BffRedisKeys.prefix(appId));
        return decode(raw, BffSession.class);
    }

    /** 首次携带实际正式 Cookie 的成功请求确认交付，不新建身份、不续期。 */
    public boolean confirm(BffSession session) {
        return run(BffRedisScripts.CONFIRM_SESSION,
                List.of(BffRedisKeys.session(session.getAppId(), session.getSessionId()),
                        BffRedisKeys.delivered(session.getAppId(), session.getSessionId()),
                        BffRedisKeys.current(session.getAppId(), session.getBindingId())),
                session.getAppId(), Long.toString(now())) == 1;
    }

    /** 注销/替换立即使 BFF 会话不可用，并持久登记远端撤销。 */
    public void invalidate(BffSession session) {
        run(BffRedisScripts.INVALIDATE_SESSION,
                List.of(BffRedisKeys.session(session.getAppId(), session.getSessionId()),
                        BffRedisKeys.delivered(session.getAppId(), session.getSessionId()),
                        BffRedisKeys.current(session.getAppId(), session.getBindingId()),
                        BffRedisKeys.cleanup(session.getAppId()), BffRedisKeys.cleanupDue(session.getAppId())),
                Long.toString(now()));
    }

    /** CAS 失败时仍登记本工作者已知 sid，不能只删除事务。 */
    public void enqueueRevoke(BffSession session) {
        BffCleanup job = cleanupOf(session, false);
        run(BffRedisScripts.ENQUEUE_CLEANUP,
                List.of(BffRedisKeys.cleanup(session.getAppId()), BffRedisKeys.cleanupDue(session.getAppId()),
                        BffRedisKeys.session(session.getAppId(), session.getSessionId())),
                session.getSessionId(), encode(job), Long.toString(now()));
    }

    /** 持久清理载荷总量；供后台低频监控，不在请求中全量扫描。 */
    public long cleanupSize(String appId) {
        return redis.opsForHash().size(BffRedisKeys.cleanup(appId));
    }

    /** 有界读取到期任务；实际执行再使用租约和条件脚本确认所有权。 */
    public Set<String> due(String appId) {
        Set<String> values = redis.opsForZSet()
            .rangeByScore(BffRedisKeys.cleanupDue(appId), 0, now(), 0, BffConstants.CLEANUP_BATCH_SIZE);
        return values == null ? Set.of() : values;
    }

    /** 清理者取得租约后才可触及载荷。 */
    public BffCleanup claimCleanup(String appId, String id, String owner) {
        if (!Boolean.TRUE.equals(redis.opsForValue()
            .setIfAbsent(BffRedisKeys.cleanupLease(appId, id), owner,
                    Duration.ofSeconds(BffConstants.CLEANUP_LEASE_SECONDS))))
            return null;
        Object raw = redis.opsForHash().get(BffRedisKeys.cleanup(appId), id);
        BffCleanup job = decode(raw == null ? null : raw.toString(), BffCleanup.class);
        if (job == null) {
            redis.opsForZSet().remove(BffRedisKeys.cleanupDue(appId), id);
            return null;
        }
        job.setPendingDelivery(false);
        long result = run(BffRedisScripts.PREPARE_CLEANUP,
                List.of(BffRedisKeys.cleanup(appId), BffRedisKeys.cleanupDue(appId), BffRedisKeys.delivered(appId, id),
                        BffRedisKeys.session(appId, id), BffRedisKeys.current(appId, job.getBindingId()),
                        BffRedisKeys.transaction(appId, job.getTransactionId()), BffRedisKeys.cleanupLease(appId, id)),
                owner, id, encode(job));
        return result == 1 ? job : null;
    }

    /** 仅原租约持有者在 Auth 撤销成功后移除任务。 */
    public void finishCleanup(BffCleanup job, String owner) {
        run(BffRedisScripts.FINISH_CLEANUP, List.of(BffRedisKeys.cleanup(job.getAppId()),
                BffRedisKeys.cleanupDue(job.getAppId()), BffRedisKeys.cleanupLease(job.getAppId(), job.getSessionId())),
                owner, job.getSessionId());
    }

    private long run(String script, List<String> keys, String... args) {
        Long value = redis.execute(new DefaultRedisScript<>(script, Long.class), keys, (Object[]) args);
        return value == null ? 0 : value;
    }

    private String txKey(LoginTransaction tx) {
        return BffRedisKeys.transaction(tx.getAppId(), tx.getTransactionId());
    }

    private String activeKey(LoginTransaction tx) {
        return BffRedisKeys.active(tx.getAppId(), tx.getBindingId());
    }

    private String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("BFF 安全状态序列化失败", exception);
        }
    }

    private <T> T decode(String value, Class<T> type) {
        if (value == null)
            return null;
        try {
            return mapper.readValue(value, type);
        }
        catch (JsonProcessingException exception) {
            throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
    }

    private BffCleanup cleanupOf(BffSession s, boolean pending) {
        BffCleanup job = new BffCleanup();
        job.setAppId(s.getAppId());
        job.setBindingId(s.getBindingId());
        job.setSessionId(s.getSessionId());
        job.setTransactionId(s.getTransactionId());
        job.setSid(s.getSid());
        job.setAuthCookie(s.getAuthCookie());
        job.setPendingDelivery(pending);
        return job;
    }

}
