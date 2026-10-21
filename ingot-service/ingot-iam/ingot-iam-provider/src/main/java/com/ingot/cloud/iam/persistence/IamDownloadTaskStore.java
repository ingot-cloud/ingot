package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.identity.InitializationIdAllocator;
import com.ingot.cloud.iam.persistence.entity.IamDownloadTaskEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDownloadTaskMapper;
import com.ingot.framework.commons.model.download.DownloadTaskStatus;
import com.ingot.framework.download.DownloadOwner;
import com.ingot.framework.download.DownloadTaskRecord;
import com.ingot.framework.download.DownloadTaskStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>IAM 承载的下载中心任务存储，归属读取同时绑定认证域、租户与成员。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class IamDownloadTaskStore implements DownloadTaskStore {
    private final IamDownloadTaskMapper mapper;
    private final InitializationIdAllocator ids;

    @Override
    public long nextId() {
        return ids.nextId();
    }

    @Override
    public void insert(DownloadTaskRecord task) {
        IamDownloadTaskEntity row = new IamDownloadTaskEntity();
        row.setId(BigInteger.valueOf(task.id()));
        row.setDomain(task.owner().domain());
        row.setTenantId(task.owner().tenantId() == null ? null : BigInteger.valueOf(task.owner().tenantId()));
        row.setActorMemberId(BigInteger.valueOf(task.owner().memberId()));
        row.setSource(task.source());
        row.setTitle(task.title());
        row.setStatus(task.status());
        row.setCreatedAt(utc(task.createdAt()));
        row.setExpiresAt(utc(task.expiresAt()));
        mapper.insert(row);
    }

    @Override
    public boolean markRunning(long id, Instant startedAt) {
        return mapper.update(Wrappers.<IamDownloadTaskEntity>lambdaUpdate()
                .eq(IamDownloadTaskEntity::getId, BigInteger.valueOf(id))
                .eq(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.PENDING)
                .set(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.RUNNING)
                .set(IamDownloadTaskEntity::getStartedAt, utc(startedAt))) == 1;
    }

    @Override
    public boolean markSucceeded(long id, String filename, String contentType, long fileSize, String objectPath,
            Instant completedAt) {
        return mapper.update(Wrappers.<IamDownloadTaskEntity>lambdaUpdate()
                .eq(IamDownloadTaskEntity::getId, BigInteger.valueOf(id))
                .eq(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.RUNNING)
                .set(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.SUCCEEDED)
                .set(IamDownloadTaskEntity::getFilename, filename)
                .set(IamDownloadTaskEntity::getContentType, contentType)
                .set(IamDownloadTaskEntity::getFileSize, fileSize)
                .set(IamDownloadTaskEntity::getObjectPath, objectPath)
                .set(IamDownloadTaskEntity::getCompletedAt, utc(completedAt))) == 1;
    }

    @Override
    public boolean markFailed(long id, String reason, Instant completedAt) {
        return mapper.update(Wrappers.<IamDownloadTaskEntity>lambdaUpdate()
                .eq(IamDownloadTaskEntity::getId, BigInteger.valueOf(id))
                .in(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.PENDING, DownloadTaskStatus.RUNNING)
                .set(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.FAILED)
                .set(IamDownloadTaskEntity::getFailureReason, reason)
                .set(IamDownloadTaskEntity::getCompletedAt, utc(completedAt))) == 1;
    }

    @Override
    public DownloadTaskRecord find(DownloadOwner owner, long id) {
        IamDownloadTaskEntity row = mapper.selectOne(owned(owner)
                .eq(IamDownloadTaskEntity::getId, BigInteger.valueOf(id)));
        return row == null ? null : record(row);
    }

    @Override
    public long count(DownloadOwner owner) {
        Long total = mapper.selectCount(owned(owner));
        return total == null ? 0 : total;
    }

    @Override
    public List<DownloadTaskRecord> page(DownloadOwner owner, long offset, int limit) {
        return mapper.selectList(owned(owner)
                        .orderByDesc(IamDownloadTaskEntity::getCreatedAt)
                        .orderByDesc(IamDownloadTaskEntity::getId)
                        .last("LIMIT " + Math.max(1, limit) + " OFFSET " + Math.max(0, offset)))
                .stream().map(IamDownloadTaskStore::record).toList();
    }

    @Override
    public List<DownloadTaskRecord> listExpired(Instant now, int limit) {
        return mapper.selectList(Wrappers.<IamDownloadTaskEntity>lambdaQuery()
                        .eq(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.SUCCEEDED)
                        .le(IamDownloadTaskEntity::getExpiresAt, utc(now))
                        .orderByAsc(IamDownloadTaskEntity::getExpiresAt)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream().map(IamDownloadTaskStore::record).toList();
    }

    @Override
    public boolean markExpired(long id) {
        return mapper.update(Wrappers.<IamDownloadTaskEntity>lambdaUpdate()
                .eq(IamDownloadTaskEntity::getId, BigInteger.valueOf(id))
                .eq(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.SUCCEEDED)
                .set(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.EXPIRED)
                .set(IamDownloadTaskEntity::getObjectPath, null)) == 1;
    }

    @Override
    public int failStale(Instant createdBefore, String reason, Instant completedAt) {
        return mapper.update(Wrappers.<IamDownloadTaskEntity>lambdaUpdate()
                .in(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.PENDING, DownloadTaskStatus.RUNNING)
                .lt(IamDownloadTaskEntity::getCreatedAt, utc(createdBefore))
                .set(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.FAILED)
                .set(IamDownloadTaskEntity::getFailureReason, reason)
                .set(IamDownloadTaskEntity::getCompletedAt, utc(completedAt)));
    }

    @Override
    public int purge(Instant createdBefore) {
        return mapper.delete(Wrappers.<IamDownloadTaskEntity>lambdaQuery()
                .in(IamDownloadTaskEntity::getStatus, DownloadTaskStatus.FAILED, DownloadTaskStatus.EXPIRED)
                .lt(IamDownloadTaskEntity::getCreatedAt, utc(createdBefore)));
    }

    private static LambdaQueryWrapper<IamDownloadTaskEntity> owned(DownloadOwner owner) {
        LambdaQueryWrapper<IamDownloadTaskEntity> query = Wrappers.<IamDownloadTaskEntity>lambdaQuery()
                .eq(IamDownloadTaskEntity::getDomain, owner.domain())
                .eq(IamDownloadTaskEntity::getActorMemberId, BigInteger.valueOf(owner.memberId()));
        if (owner.tenantId() == null) {
            query.isNull(IamDownloadTaskEntity::getTenantId);
        } else {
            query.eq(IamDownloadTaskEntity::getTenantId, BigInteger.valueOf(owner.tenantId()));
        }
        return query;
    }

    private static DownloadTaskRecord record(IamDownloadTaskEntity row) {
        DownloadOwner owner = new DownloadOwner(row.getDomain(),
                row.getTenantId() == null ? null : row.getTenantId().longValueExact(),
                row.getActorMemberId().longValueExact());
        return new DownloadTaskRecord(row.getId().longValueExact(), owner, row.getSource(), row.getTitle(),
                row.getStatus(), row.getFilename(), row.getContentType(), row.getFileSize(), row.getObjectPath(),
                row.getFailureReason(), instant(row.getCreatedAt()), instant(row.getStartedAt()),
                instant(row.getCompletedAt()), instant(row.getExpiresAt()));
    }

    private static LocalDateTime utc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
