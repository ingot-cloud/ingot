package com.ingot.framework.cache.derived;

import com.ingot.framework.cache.source.CacheSource;

/**
 * <p>派生缓存的失效键，由数据来源与版本号共同构成。</p>
 *
 * <p>必须是二元组而不能只比版本号：地板快照的版本恒为 0，远端版本来自数据库，降级与恢复之间会出现版本
 * 回退甚至相等。若只比数值，「远端(v0 尚无数据) → 地板(v0) → 远端恢复(v0)」这类序列会漏掉重新编译。
 * 把来源纳入键即可彻底消除歧义。</p>
 *
 * @param source  数据来源
 * @param version 快照版本号
 * @author jy
 * @since 1.0.0
 * @see VersionedDerivedCache
 */
public record SnapshotVersion(CacheSource source, long version) {
}
