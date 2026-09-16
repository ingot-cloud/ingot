package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamMemberExportEntity;
import com.ingot.cloud.iam.persistence.mapper.IamMemberExportMapper;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护成员导出任务行，所有读写绑定可信 tenantId，快照只保存成员 ID。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberExportRepository {
    private static final TypeReference<List<String>> MEMBER_IDS = new TypeReference<>() {
    };
    private final IamMemberExportMapper mapper;

    /**
     * 插入待执行任务。
     *
     * @param row 已填充标识与过期时间的任务
     */
    public void insert(IamMemberExportEntity row) {
        mapper.insert(row);
    }

    /**
     * 按租户读取任务。
     *
     * @param tenantId 已授权租户
     * @param exportId 任务 ID
     * @return 任务行；不存在时为空
     */
    public IamMemberExportEntity find(long tenantId, long exportId) {
        return mapper.selectOne(Wrappers.<IamMemberExportEntity>lambdaQuery()
                .eq(IamMemberExportEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberExportEntity::getId, BigInteger.valueOf(exportId)));
    }

    /**
     * 把待执行任务标为运行中，避免多实例重复快照。
     *
     * @param tenantId 已授权租户
     * @param exportId 任务 ID
     * @return 恰好抢到一行时为 true
     */
    public boolean markRunning(long tenantId, long exportId) {
        return mapper.update(Wrappers.<IamMemberExportEntity>lambdaUpdate()
                .eq(IamMemberExportEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberExportEntity::getId, BigInteger.valueOf(exportId))
                .eq(IamMemberExportEntity::getStatus, ExportTaskStatus.PENDING)
                .set(IamMemberExportEntity::getStatus, ExportTaskStatus.RUNNING)) == 1;
    }

    /**
     * 写入成功快照并保留既有过期时间。
     *
     * @param tenantId 已授权租户
     * @param exportId 任务 ID
     * @param memberIds 完整成员 ID
     * @param completedAt 完成时间
     * @return 恰好更新运行中任务时为 true
     */
    public boolean markSucceeded(long tenantId, long exportId, List<String> memberIds, LocalDateTime completedAt) {
        return mapper.update(Wrappers.<IamMemberExportEntity>lambdaUpdate()
                .eq(IamMemberExportEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberExportEntity::getId, BigInteger.valueOf(exportId))
                .eq(IamMemberExportEntity::getStatus, ExportTaskStatus.RUNNING)
                .set(IamMemberExportEntity::getStatus, ExportTaskStatus.SUCCEEDED)
                .set(IamMemberExportEntity::getMemberIds, IamJson.array(memberIds))
                .set(IamMemberExportEntity::getFailureReason, null)
                .set(IamMemberExportEntity::getCompletedAt, completedAt)) == 1;
    }

    /**
     * 记录失败编码，不写入成员 ID。
     *
     * @param tenantId 已授权租户
     * @param exportId 任务 ID
     * @param reason 稳定失败编码
     * @param completedAt 完成时间
     */
    public void markFailed(long tenantId, long exportId, String reason, LocalDateTime completedAt) {
        mapper.update(Wrappers.<IamMemberExportEntity>lambdaUpdate()
                .eq(IamMemberExportEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberExportEntity::getId, BigInteger.valueOf(exportId))
                .in(IamMemberExportEntity::getStatus, ExportTaskStatus.PENDING, ExportTaskStatus.RUNNING)
                .set(IamMemberExportEntity::getStatus, ExportTaskStatus.FAILED)
                .set(IamMemberExportEntity::getMemberIds, null)
                .set(IamMemberExportEntity::getFailureReason, reason)
                .set(IamMemberExportEntity::getCompletedAt, completedAt));
    }

    /**
     * 把已到期且仍可下载的任务标为过期并清除快照。
     *
     * @param tenantId 已授权租户
     * @param now UTC 当前时间
     */
    public void expireDue(long tenantId, LocalDateTime now) {
        mapper.update(Wrappers.<IamMemberExportEntity>lambdaUpdate()
                .eq(IamMemberExportEntity::getTenantId, BigInteger.valueOf(tenantId))
                .le(IamMemberExportEntity::getExpiresAt, now)
                .in(IamMemberExportEntity::getStatus, ExportTaskStatus.PENDING, ExportTaskStatus.RUNNING,
                        ExportTaskStatus.SUCCEEDED)
                .set(IamMemberExportEntity::getStatus, ExportTaskStatus.EXPIRED)
                .set(IamMemberExportEntity::getMemberIds, null)
                .set(IamMemberExportEntity::getFailureReason, null));
    }

    /**
     * 解析成功快照中的成员 ID；非法 JSON 视为空。
     *
     * @param row 任务行
     * @return 成员 ID，保持写入顺序
     */
    public static List<String> memberIds(IamMemberExportEntity row) {
        if (row == null || row.getMemberIds() == null || row.getMemberIds().isBlank()) {
            return List.of();
        }
        List<String> ids = IamJson.read(row.getMemberIds(), MEMBER_IDS);
        return ids == null ? List.of() : List.copyOf(ids);
    }
}
