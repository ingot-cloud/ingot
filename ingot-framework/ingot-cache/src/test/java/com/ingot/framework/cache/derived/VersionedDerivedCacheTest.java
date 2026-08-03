package com.ingot.framework.cache.derived;

import java.util.concurrent.atomic.AtomicInteger;

import com.ingot.framework.cache.source.CacheSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link VersionedDerivedCache} 的版本驱动重编译行为验证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class VersionedDerivedCacheTest {

    private record Snapshot(CacheSource source, long version, String payload) {
    }

    private final AtomicInteger compileCount = new AtomicInteger();

    private VersionedDerivedCache<Snapshot, String> newCache() {
        compileCount.set(0);
        return new VersionedDerivedCache<>(
                s -> new SnapshotVersion(s.source(), s.version()),
                s -> {
                    compileCount.incrementAndGet();
                    return "compiled:" + s.payload();
                });
    }

    @Test
    @DisplayName("版本未变化时复用旧编译产物")
    void reusesWhenVersionUnchanged() {
        VersionedDerivedCache<Snapshot, String> cache = newCache();
        Snapshot first = new Snapshot(CacheSource.REMOTE, 7L, "a");
        Snapshot second = new Snapshot(CacheSource.REMOTE, 7L, "b");

        assertThat(cache.get(first)).isEqualTo("compiled:a");
        assertThat(cache.get(second)).isEqualTo("compiled:a");
        assertThat(compileCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("版本递增触发重编译")
    void recompilesWhenVersionIncreases() {
        VersionedDerivedCache<Snapshot, String> cache = newCache();

        cache.get(new Snapshot(CacheSource.REMOTE, 1L, "a"));
        assertThat(cache.get(new Snapshot(CacheSource.REMOTE, 2L, "b"))).isEqualTo("compiled:b");
        assertThat(compileCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("降级导致版本回退时仍触发重编译")
    void recompilesWhenVersionGoesBackwards() {
        VersionedDerivedCache<Snapshot, String> cache = newCache();

        cache.get(new Snapshot(CacheSource.REMOTE, 42L, "remote"));
        String floor = cache.get(new Snapshot(CacheSource.LOCAL_FLOOR, 0L, "floor"));

        assertThat(floor).isEqualTo("compiled:floor");
        assertThat(compileCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("版本号相等但来源不同时触发重编译")
    void recompilesWhenOnlySourceDiffers() {
        VersionedDerivedCache<Snapshot, String> cache = newCache();

        // 远端尚无数据（version=0）→ 降级地板（version 同为 0）→ 远端恢复（version 仍为 0）。
        // 只比对数值会漏掉这两次切换，二元组键才能正确识别。
        cache.get(new Snapshot(CacheSource.REMOTE, 0L, "remote-empty"));
        cache.get(new Snapshot(CacheSource.LOCAL_FLOOR, 0L, "floor"));
        String back = cache.get(new Snapshot(CacheSource.REMOTE, 0L, "remote-again"));

        assertThat(back).isEqualTo("compiled:remote-again");
        assertThat(compileCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("LKG 与远端版本号相同也视为不同来源")
    void distinguishesLastKnownGoodFromRemote() {
        VersionedDerivedCache<Snapshot, String> cache = newCache();

        cache.get(new Snapshot(CacheSource.REMOTE, 5L, "remote"));
        cache.get(new Snapshot(CacheSource.LAST_KNOWN_GOOD, 5L, "lkg"));

        assertThat(compileCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("evictAll 后强制重编译并清空当前版本")
    void evictAllForcesRecompile() {
        VersionedDerivedCache<Snapshot, String> cache = newCache();
        Snapshot snapshot = new Snapshot(CacheSource.REMOTE, 1L, "a");

        cache.get(snapshot);
        assertThat(cache.currentVersion()).isEqualTo(new SnapshotVersion(CacheSource.REMOTE, 1L));

        cache.evictAll();
        assertThat(cache.currentVersion()).isNull();

        cache.get(snapshot);
        assertThat(compileCount.get()).isEqualTo(2);
    }
}
