package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDepartmentWorkbookEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentWorkbookMapper;
import com.ingot.cloud.iam.domain.workbook.DepartmentWorkbookLimits;
import com.ingot.cloud.iam.domain.download.ExportTaskStatus;
import com.ingot.cloud.iam.domain.workbook.MemberWorkbookKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护部门表格导入任务行，所有读写绑定可信 tenantId 与发起成员。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class DepartmentWorkbookRepository {
    private final IamDepartmentWorkbookMapper mapper;

    /**
     * 插入待执行任务。
     *
     * @param row 已填充标识与过期时间的任务
     */
    public void insert(IamDepartmentWorkbookEntity row) {
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
    public IamDepartmentWorkbookEntity findOwned(long tenantId, long actorMemberId, long workbookId) {
        return mapper.selectOne(Wrappers.<IamDepartmentWorkbookEntity>lambdaQuery()
                .eq(IamDepartmentWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentWorkbookEntity::getActorMemberId, BigInteger.valueOf(actorMemberId))
                .eq(IamDepartmentWorkbookEntity::getId, BigInteger.valueOf(workbookId)));
    }

    /**
     * 返回当前操作者最近的导入任务，新的在前。
     *
     * @param tenantId 已授权租户
     * @param actorMemberId 发起成员
     * @return 最多 {@link DepartmentWorkbookLimits#HISTORY_SIZE} 条
     */
    public List<IamDepartmentWorkbookEntity> listRecentImports(long tenantId, long actorMemberId) {
        return mapper.selectList(Wrappers.<IamDepartmentWorkbookEntity>lambdaQuery()
                .eq(IamDepartmentWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentWorkbookEntity::getActorMemberId, BigInteger.valueOf(actorMemberId))
                .eq(IamDepartmentWorkbookEntity::getKind, MemberWorkbookKind.IMPORT)
                .orderByDesc(IamDepartmentWorkbookEntity::getCreatedAt)
                .orderByDesc(IamDepartmentWorkbookEntity::getId)
                .last("LIMIT " + DepartmentWorkbookLimits.HISTORY_SIZE));
    }

    /**
     * 把待执行任务标为运行中，避免多实例重复处理。
     *
     * @param tenantId 已授权租户
     * @param workbookId 任务 ID
     * @return 恰好抢到一行时为 true
     */
    public boolean markRunning(long tenantId, long workbookId) {
        return mapper.update(Wrappers.<IamDepartmentWorkbookEntity>lambdaUpdate()
                .eq(IamDepartmentWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentWorkbookEntity::getId, BigInteger.valueOf(workbookId))
                .eq(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.PENDING)
                .set(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.RUNNING)) == 1;
    }

    /**
     * 写入成功产物路径并保留既有过期时间。
     *
     * @param tenantId 已授权租户
     * @param workbookId 任务 ID
     * @param result 计数 JSON
     * @param workbookPath 工作簿路径
     * @param errorReportPath 错误报告路径
     * @param completedAt 完成时间
     * @return 恰好更新运行中任务时为 true
     */
    public boolean markSucceeded(long tenantId, long workbookId, String result, String workbookPath,
            String errorReportPath, LocalDateTime completedAt) {
        return mapper.update(Wrappers.<IamDepartmentWorkbookEntity>lambdaUpdate()
                .eq(IamDepartmentWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentWorkbookEntity::getId, BigInteger.valueOf(workbookId))
                .eq(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.RUNNING)
                .set(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.SUCCEEDED)
                .set(IamDepartmentWorkbookEntity::getResult, result)
                .set(IamDepartmentWorkbookEntity::getWorkbookPath, workbookPath)
                .set(IamDepartmentWorkbookEntity::getErrorReportPath, errorReportPath)
                .set(IamDepartmentWorkbookEntity::getFailureReason, null)
                .set(IamDepartmentWorkbookEntity::getCompletedAt, completedAt)) == 1;
    }

    /**
     * 记录整份失败编码。
     *
     * @param tenantId 已授权租户
     * @param workbookId 任务 ID
     * @param reason 稳定失败编码
     * @param completedAt 完成时间
     */
    public void markFailed(long tenantId, long workbookId, String reason, LocalDateTime completedAt) {
        mapper.update(Wrappers.<IamDepartmentWorkbookEntity>lambdaUpdate()
                .eq(IamDepartmentWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentWorkbookEntity::getId, BigInteger.valueOf(workbookId))
                .in(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.PENDING, ExportTaskStatus.RUNNING)
                .set(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.FAILED)
                .set(IamDepartmentWorkbookEntity::getDepartmentIds, null)
                .set(IamDepartmentWorkbookEntity::getResult, null)
                .set(IamDepartmentWorkbookEntity::getFailureReason, reason)
                .set(IamDepartmentWorkbookEntity::getCompletedAt, completedAt));
    }

    /**
     * 列出已到期且仍可下载的任务，供清理对象后再标过期。
     *
     * @param tenantId 已授权租户
     * @param now UTC 当前时间
     * @return 到期任务
     */
    public List<IamDepartmentWorkbookEntity> listDue(long tenantId, LocalDateTime now) {
        return mapper.selectList(Wrappers.<IamDepartmentWorkbookEntity>lambdaQuery()
                .eq(IamDepartmentWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .le(IamDepartmentWorkbookEntity::getExpiresAt, now)
                .in(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.PENDING, ExportTaskStatus.RUNNING,
                        ExportTaskStatus.SUCCEEDED));
    }

    /**
     * 把已到期且仍可下载的任务标为过期并清空路径。
     *
     * @param tenantId 已授权租户
     * @param now UTC 当前时间
     * @return 被标过期的任务，便于删除对象
     */
    public List<IamDepartmentWorkbookEntity> expireDue(long tenantId, LocalDateTime now) {
        List<IamDepartmentWorkbookEntity> due = new ArrayList<>(listDue(tenantId, now));
        if (due.isEmpty()) {
            return List.of();
        }
        mapper.update(Wrappers.<IamDepartmentWorkbookEntity>lambdaUpdate()
                .eq(IamDepartmentWorkbookEntity::getTenantId, BigInteger.valueOf(tenantId))
                .le(IamDepartmentWorkbookEntity::getExpiresAt, now)
                .in(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.PENDING, ExportTaskStatus.RUNNING,
                        ExportTaskStatus.SUCCEEDED)
                .set(IamDepartmentWorkbookEntity::getStatus, ExportTaskStatus.EXPIRED)
                .set(IamDepartmentWorkbookEntity::getWorkbookPath, null)
                .set(IamDepartmentWorkbookEntity::getErrorReportPath, null)
                .set(IamDepartmentWorkbookEntity::getFailureReason, null));
        return due;
    }
}
