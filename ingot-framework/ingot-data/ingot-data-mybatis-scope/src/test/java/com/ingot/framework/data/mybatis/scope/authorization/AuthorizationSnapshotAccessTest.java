package com.ingot.framework.data.mybatis.scope.authorization;

import java.time.Instant;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import com.ingot.framework.data.mybatis.scope.error.AuthorizationSnapshotException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** <p>验证旧租户消费快照在线读取及平台热缓存隔离。</p> @author jy @since 1.0.0 */
class AuthorizationSnapshotAccessTest {
    @Test
    @SuppressWarnings("unchecked")
    void tenantLoadsEachTimeAndRemoteFailureNeverUsesOldAllow() {
        LayeredCache<String,AuthorizationSnapshotDTO> cache=mock(LayeredCache.class);
        AuthorizationSnapshotLoader loader=mock(AuthorizationSnapshotLoader.class);
        var snapshot=new AuthorizationSnapshotDTO();snapshot.setExpiresAt(Instant.now().plusSeconds(30));
        when(loader.load(10,1)).thenReturn(snapshot).thenThrow(new RemoteUnavailableException("fixture outage"));
        var access=new AuthorizationSnapshotAccess(cache,loader);
        assertSame(snapshot,access.require(10,1));assertThrows(AuthorizationSnapshotException.class,()->access.require(10,1));
        verify(loader,times(2)).load(10,1);verifyNoInteractions(cache);
    }
    @Test
    @SuppressWarnings("unchecked")
    void platformRetainsExistingCachePath() {
        LayeredCache<String,AuthorizationSnapshotDTO> cache=mock(LayeredCache.class);
        AuthorizationSnapshotLoader loader=mock(AuthorizationSnapshotLoader.class);
        var snapshot=new AuthorizationSnapshotDTO();snapshot.setExpiresAt(Instant.now().plusSeconds(30));when(cache.get("0:1")).thenReturn(snapshot);
        assertSame(snapshot,new AuthorizationSnapshotAccess(cache,loader).require(0,1));verifyNoInteractions(loader);
    }
}
