package com.ingot.cloud.iam.infrastructure.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.ingot.cloud.iam.domain.role.RoleRevisionSnapshot;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;

/**
 * <p>按既有JSON载荷计算角色合成指纹，与领域快照的结构职责分离。</p>
 * @author jy
 * @since 1.0.0
 */
public final class RoleRevisionFingerprint {
    private RoleRevisionFingerprint() { }
    /**
     * 计算包含基础与差异来源及内容的版本指纹，供派生缓存判定是否重编译。
     *
     * @return 64 位指纹
     */
    public static long of(RoleRevisionSnapshot snapshot) {
        String payload = snapshot.revisionId() + ":" + (snapshot.baseRevisionId() == null ? "0" : snapshot.baseRevisionId()) + ":"
                + IamJsonCodec.array(snapshot.baseGrants()) + ":" + IamJsonCodec.array(snapshot.deltas());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            long value = 0;
            for (int index = 0; index < 8; index++) {
                value = (value << 8) | (digest[index] & 0xffL);
            }
            return value;
        } catch (NoSuchAlgorithmException exception) {
            return payload.hashCode();
        }
    }

}
