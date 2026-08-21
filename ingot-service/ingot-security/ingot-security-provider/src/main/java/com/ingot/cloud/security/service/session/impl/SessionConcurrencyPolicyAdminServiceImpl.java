package com.ingot.cloud.security.service.session.impl;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.mapper.SessionConcurrencyPolicyMapper;
import com.ingot.cloud.security.model.domain.SessionConcurrencyPolicy;
import com.ingot.cloud.security.service.policy.SecurityPolicyChangedSpringEvent;
import com.ingot.cloud.security.service.session.SessionConcurrencyPolicyAdminService;
import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionPolicyScope;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>并发会话策略管理面服务实现。</p>
 *
 * <p>写入前统一做「范围字段归一 + 唯一性校验」：不适用的定位字段一律落空串，使唯一索引
 * {@code (scope, client_id, user_type)} 真正生效；缺省值在此补齐，让 Auth 侧拿到的策略
 * 永远是完整的，不必再处理 {@code null}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SessionConcurrencyPolicyAdminServiceImpl implements SessionConcurrencyPolicyAdminService {

    private final SessionConcurrencyPolicyMapper policyMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AssertionChecker assertionChecker;

    @Override
    public List<SessionConcurrencyPolicy> list() {
        return policyMapper.selectList(Wrappers.<SessionConcurrencyPolicy>lambdaQuery()
                .orderByAsc(SessionConcurrencyPolicy::getScope)
                .orderByAsc(SessionConcurrencyPolicy::getClientId)
                .orderByAsc(SessionConcurrencyPolicy::getUserType));
    }

    @Override
    public SessionConcurrencyPolicy getById(Long id) {
        assertionChecker.checkOperation(id != null, "SecurityPolicy.IdNotNull");
        SessionConcurrencyPolicy policy = policyMapper.selectById(id);
        assertionChecker.checkOperation(policy != null, "SecurityPolicy.SessionPolicyNotFound");
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionConcurrencyPolicy create(SessionConcurrencyPolicy policy) {
        normalize(policy);
        checkUnique(policy, null);
        policy.setId(null);
        policy.setCreatedAt(DateUtil.now());
        policy.setUpdatedAt(DateUtil.now());
        policyMapper.insert(policy);
        publishChanged();
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionConcurrencyPolicy update(SessionConcurrencyPolicy policy) {
        assertionChecker.checkOperation(policy != null && policy.getId() != null, "SecurityPolicy.IdNotNull");
        SessionConcurrencyPolicy existing = getById(policy.getId());
        normalize(policy);
        checkUnique(policy, policy.getId());
        policy.setCreatedAt(existing.getCreatedAt());
        policy.setUpdatedAt(DateUtil.now());
        policyMapper.updateById(policy);
        publishChanged();
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SessionConcurrencyPolicy existing = getById(id);
        assertionChecker.checkOperation(existing.getScope() != SessionPolicyScope.GLOBAL,
                "SecurityPolicy.SessionGlobalPolicyCantDelete");
        policyMapper.deleteById(id);
        publishChanged();
    }

    /**
     * 校验必填项并补齐缺省值，同时把不适用的范围定位字段落为空串。
     */
    private void normalize(SessionConcurrencyPolicy policy) {
        assertionChecker.checkOperation(policy != null, "SecurityPolicy.SessionPolicyNotNull");
        assertionChecker.checkOperation(policy.getScope() != null, "SecurityPolicy.SessionScopeNotNull");
        assertionChecker.checkOperation(policy.getMaxSessions() != null && policy.getMaxSessions() >= 0,
                "SecurityPolicy.SessionMaxSessionsInvalid");

        if (policy.getScope() == SessionPolicyScope.CLIENT) {
            assertionChecker.checkOperation(StrUtil.isNotBlank(policy.getClientId()),
                    "SecurityPolicy.SessionClientIdRequired");
            policy.setClientId(StrUtil.trim(policy.getClientId()));
        } else {
            policy.setClientId(StrUtil.EMPTY);
        }

        if (policy.getScope() == SessionPolicyScope.USER_TYPE) {
            assertionChecker.checkOperation(UserTypeEnum.getEnum(StrUtil.trim(policy.getUserType())) != null,
                    "SecurityPolicy.SessionUserTypeInvalid");
            policy.setUserType(StrUtil.trim(policy.getUserType()));
        } else {
            policy.setUserType(StrUtil.EMPTY);
        }

        if (policy.getDimension() == null) {
            policy.setDimension(SessionConcurrencyDimension.USER_CLIENT);
        }
        if (policy.getOverflow() == null) {
            policy.setOverflow(SessionOverflowStrategy.KICK_OLDEST);
        }
        if (policy.getAdminForbidConcurrent() == null) {
            policy.setAdminForbidConcurrent(Boolean.FALSE);
        }
        if (policy.getEnabled() == null) {
            policy.setEnabled(Boolean.TRUE);
        }
    }

    /**
     * 同一 {@code (scope, clientId, userType)} 只允许一条策略。
     *
     * @param excludeId 更新场景下排除自身；新增传 {@code null}
     */
    private void checkUnique(SessionConcurrencyPolicy policy, Long excludeId) {
        Long count = policyMapper.selectCount(Wrappers.<SessionConcurrencyPolicy>lambdaQuery()
                .eq(SessionConcurrencyPolicy::getScope, policy.getScope())
                .eq(SessionConcurrencyPolicy::getClientId, policy.getClientId())
                .eq(SessionConcurrencyPolicy::getUserType, policy.getUserType())
                .ne(excludeId != null, SessionConcurrencyPolicy::getId, excludeId));
        assertionChecker.checkOperation(count == null || count == 0, "SecurityPolicy.SessionPolicyDuplicated");
    }

    private void publishChanged() {
        eventPublisher.publishEvent(
                new SecurityPolicyChangedSpringEvent(this, SecurityPolicyDomain.SESSION_CONCURRENCY));
    }
}
