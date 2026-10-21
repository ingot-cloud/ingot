package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamMemberWorkbookEntity;
import com.ingot.cloud.iam.persistence.mapper.IamMemberWorkbookMapper;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.ingot.framework.commons.model.iam.MemberWorkbookKind;
import com.ingot.framework.commons.model.iam.MemberWorkbookLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护成员表格任务行，所有读写绑定可信 tenantId 与发起成员。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberWorkbookRepository {
    private final IamMemberWorkbookMapper mapper;

    /**
     * 插入待执行任务。
     *
     * @param row 已填充标识与过期时间的任务
     */
    public void insert(IamMemberWorkbookEntity row) {
        mapper.insert(row);
    }

    /**
     * 按租户与发起成员读取任务。
     *
     * @param tenantId 已授权租户
     * @param actorMemberId 发起成员
     * @param workbookId 任务 ID
     * @return 任务行；不存在时为空
     */
    public IamMemberWorkbookEntity findOwned(long tenantId, long actorMemberId, long workbookId) {
        return mapper.selectOne(Wrappers.<IamMemberWorkbookEntity>lambdaQuery()
                .eq(IamMemberWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberWorkbookEntity::getActorMemberId, BigInteger.valueOf(actorMemberId))
                .eq(IamMemberWorkbookEntity::getId, BigInteger.valueOf(workbookId)));
    }

    /**
     * 返回当前操作者最近的导入任务，新的在前。
     *
     * @param tenantId 已授权租户
     * @param actorMemberId 发起成员
     * @return 最多 {@link MemberWorkbookLimits#HISTORY_SIZE} 条
     */
    public List<IamMemberWorkbookEntity> listRecentImports(long tenantId, long actorMemberId) {
        return mapper.selectList(Wrappers.<IamMemberWorkbookEntity>lambdaQuery()
                .eq(IamMemberWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberWorkbookEntity::getActorMemberId, BigInteger.valueOf(actorMemberId))
                .eq(IamMemberWorkbookEntity::getKind, MemberWorkbookKind.IMPORT)
                .orderByDesc(IamMemberWorkbookEntity::getCreatedAt)
                .orderByDesc(IamMemberWorkbookEntity::getId)
                .last("LIMIT " + MemberWorkbookLimits.HISTORY_SIZE));
    }

    /**
     * 把待执行任务标为运行中，避免多实例重复处理。
     *
     * @param tenantId 已授权租户
     * @param workbookId 任务 ID
     * @return 恰好抢到一行时为 true
     */
    public boolean markRunning(long tenantId, long workbookId) {
        return mapper.update(Wrappers.<IamMemberWorkbookEntity>lambdaUpdate()
                .eq(IamMemberWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberWorkbookEntity::getId, BigInteger.valueOf(workbookId))
                .eq(IamMemberWorkbookEntity::getStatus, ExportTaskStatus.PENDING)
                .set(IamMemberWorkbookEntity::getStatus, ExportTaskStatus.RUNNING)) == 1;
    }

    /**
     * 写入成功产物并保留既有过期时间。
     *
     * @param tenantId 已授权租户
     * @param workbookId 任务 ID
     * @param memberIds 导出范围；导入可空
     * @param result 计数 JSON
     * @param workbook 工作簿
     * @param errorReport 错误报告
     * @param passwordFile 一次性密码文件
     * @param completedAt 完成时间
     * @return 恰好更新运行中任务时为 true
     */
    public boolean markSucceeded(long tenantId, long workbookId, String memberIds, String result, byte[] workbook,
            byte[] errorReport, byte[] passwordFile, LocalDateTime completedAt) {
        return mapper.update(Wrappers.<IamMemberWorkbookEntity>lambdaUpdate()
                .eq(IamMemberWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberWorkbookEntity::getId, BigInteger.valueOf(workbookId))
                .eq(IamMemberWorkbookEntity::getStatus, ExportTaskStatus.RUNNING)
                .set(IamMemberWorkbookEntity::getStatus, ExportTaskStatus.SUCCEEDED)
                .set(IamMemberWorkbookEntity::getMemberIds, memberIds)
                .set(IamMemberWorkbookEntity::getResult, result)
                .set(IamMemberWorkbookEntity::getWorkbook, workbook)
                .set(IamMemberWorkbookEntity::getErrorReport, errorReport)
                .set(IamMemberWorkbookEntity::getPasswordFile, passwordFile)
                .set(IamMemberWorkbookEntity::getFailureReason, null)
                .set(IamMemberWorkbookEntity::getCompletedAt, completedAt)) == 1;
    }

    /**
     * 记录整份失败编码并清除产物。
     *
     * @param tenantId 已授权租户
     * @param workbookId 任务 ID
     * @param reason 稳定失败编码
     * @param completedAt 完成时间
     */
    public void markFailed(long tenantId, long workbookId, String reason, LocalDateTime completedAt) {
        mapper.update(Wrappers.<IamMemberWorkbookEntity>lambdaUpdate()
                .eq(IamMemberWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberWorkbookEntity::getId, BigInteger.valueOf(workbookId))
                .in(IamMemberWorkbookEntity::getStatus, ExportTaskStatus.PENDING, ExportTaskStatus.RUNNING)
                .set(IamMemberWorkbookEntity::getStatus, ExportTaskStatus.FAILED)
                .set(IamMemberWorkbookEntity::getMemberIds, null)
                .set(IamMemberWorkbookEntity::getResult, null)
                .set(IamMemberWorkbookEntity::getWorkbook, null)
                .set(IamMemberWorkbookEntity::getErrorReport, null)
                .set(IamMemberWorkbookEntity::getPasswordFile, null)
                .set(IamMemberWorkbookEntity::getFailureReason, reason)
                .set(IamMemberWorkbookEntity::getCompletedAt, completedAt));
    }

    /**
     * 把已到期且仍可下载的任务标为过期并清除产物。
     *
     * @param tenantId 已授权租户
     * @param now UTC 当前时间
     */
    public void expireDue(long tenantId, LocalDateTime now) {
        mapper.update(Wrappers.<IamMemberWorkbookEntity>lambdaUpdate()
                .eq(IamMemberWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .le(IamMemberWorkbookEntity::getExpiresAt, now)
                .in(IamMemberWorkbookEntity::getStatus, ExportTaskStatus.PENDING, ExportTaskStatus.RUNNING,
                        ExportTaskStatus.SUCCEEDED)
                .set(IamMemberWorkbookEntity::getStatus, ExportTaskStatus.EXPIRED)
                .set(IamMemberWorkbookEntity::getWorkbook, null)
                .set(IamMemberWorkbookEntity::getErrorReport, null)
                .set(IamMemberWorkbookEntity::getPasswordFile, null)
                .set(IamMemberWorkbookEntity::getFailureReason, null));
    }

    /**
     * 取走一次性密码文件；已下载或没有文件时返回空。
     *
     * @param tenantId 已授权租户
     * @param workbookId 任务 ID
     * @return 密码文件；不可用时为空
     */
    public byte[] consumePasswords(long tenantId, long workbookId) {
        IamMemberWorkbookEntity row = mapper.selectOne(Wrappers.<IamMemberWorkbookEntity>lambdaQuery()
                .eq(IamMemberWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberWorkbookEntity::getId, BigInteger.valueOf(workbookId)));
        if (row == null || Boolean.TRUE.equals(row.getPasswordDownloaded()) || row.getPasswordFile() == null
                || row.getPasswordFile().length == 0) {
            return null;
        }
        byte[] file = row.getPasswordFile();
        int updated = mapper.update(Wrappers.<IamMemberWorkbookEntity>lambdaUpdate()
                .eq(IamMemberWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberWorkbookEntity::getId, BigInteger.valueOf(workbookId))
                .eq(IamMemberWorkbookEntity::getPasswordDownloaded, Boolean.FALSE)
                .set(IamMemberWorkbookEntity::getPasswordDownloaded, Boolean.TRUE)
                .set(IamMemberWorkbookEntity::getPasswordFile, null));
        return updated == 1 ? file : null;
    }
}
