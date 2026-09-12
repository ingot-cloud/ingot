package com.ingot.cloud.pms.authorization.snapshot;

import com.ingot.cloud.pms.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * <p>PMS 进程内的授权快照加载器，直接走统一解析，避免自调用 Feign。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Primary
@Service
@RequiredArgsConstructor
public class LocalAuthorizationSnapshotLoader implements AuthorizationSnapshotLoader {

    private final AuthorizationSnapshotAssembler assembler;

    @Override
    public AuthorizationSnapshotDTO load(long tenantId, long userId) {
        return assembler.assemble(tenantId, userId);
    }
}
