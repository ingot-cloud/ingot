package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.model.iam.AssignmentSource;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.SubjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>为冷启动读取平台治理身份是否已建立，并写入首个平台成员的治理授权。</p>
 *
 * <p>只服务于新环境的一次性初始化：判定使用平台成员是否存在，而不是账号数量，
 * 因此已有业务身份的环境不会被重复写入。授权引用冷启动种子提供的固定系统版本，
 * 不在此处创建角色定义或版本。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class PlatformBootstrapRepository {
    private final IamAccountMapper accounts;
    private final IamPlatformMemberMapper members;
    private final IamRoleAssignmentMapper assignments;

    /**
     * 判断平台治理身份是否已建立。
     *
     * @return 存在任意平台成员时返回 {@code true}，冷启动据此整体跳过
     */
    public boolean anyPlatformMember() {
        return members.selectCount(Wrappers.<IamPlatformMemberEntity>lambdaQuery()) > 0;
    }

    /**
     * 按登录名查找未删除账号，用于复用运维已建立的账号而不是重复创建。
     *
     * @param username 登录名
     * @return 账号 ID，不存在时为空
     */
    public Optional<Long> findAccountByUsername(String username) {
        return accounts.selectList(Wrappers.<IamAccountEntity>lambdaQuery()
                        .select(IamAccountEntity::getId)
                        .eq(IamAccountEntity::getUsername, username)
                        .isNull(IamAccountEntity::getDeletedAt))
                .stream().map(row -> row.getId().longValueExact()).findFirst();
    }

    /**
     * 写入平台成员的系统治理授权，来源标记为初始化。
     *
     * @param id 授权 ID
     * @param platformMemberId 平台成员 ID
     * @param revisionId 平台域系统角色版本 ID
     */
    public void insertGovernanceAssignment(long id, long platformMemberId, long revisionId) {
        IamRoleAssignmentEntity assignment = new IamRoleAssignmentEntity();
        assignment.setId(BigInteger.valueOf(id));
        assignment.setDomain(AuthorizationDomain.PLATFORM);
        assignment.setSubjectType(SubjectType.MEMBER);
        assignment.setPlatformMemberId(BigInteger.valueOf(platformMemberId));
        assignment.setRevisionId(BigInteger.valueOf(revisionId));
        assignment.setRevisionKind(RoleKind.SYSTEM);
        assignment.setScopeBindings(IamJson.object(null));
        assignment.setValidFrom(LocalDateTime.now(ZoneOffset.UTC));
        assignment.setSource(AssignmentSource.INITIALIZATION);
        assignments.insert(assignment);
    }
}
