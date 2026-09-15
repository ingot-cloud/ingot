package com.ingot.cloud.iam.role;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.RoleDelta;

/**
 * <p>角色合成的不可变输入快照，来源包含基础版本与差异版本，指纹覆盖全部相关修订内容。</p>
 *
 * @param revisionId 当前版本
 * @param baseRevisionId 基础版本；完整定义时为空
 * @param baseGrants 基础授权
 * @param deltas 租户差异；完整定义时为空
 * @author jy
 * @since 1.0.0
 */
public record RoleRevisionSnapshot(long revisionId, Long baseRevisionId, List<ActionGrant> baseGrants,
                                   List<RoleDelta> deltas) {
    /**
     * 复制授权集合，避免缓存输入被调用方修改。
     */
    public RoleRevisionSnapshot {
        baseGrants = copyGrants(baseGrants);
        deltas = copyDeltas(deltas);
    }

    /**
     * 计算包含基础与差异来源及内容的版本指纹，供派生缓存判定是否重编译。
     *
     * @return 64 位指纹
     */
    public long fingerprint() {
        String payload = revisionId + ":" + (baseRevisionId == null ? "0" : baseRevisionId) + ":"
                + IamJson.array(baseGrants) + ":" + IamJson.array(deltas);
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

    private static List<ActionGrant> copyGrants(List<ActionGrant> grants) {
        return grants == null ? List.of() : List.copyOf(new ArrayList<>(grants));
    }

    private static List<RoleDelta> copyDeltas(List<RoleDelta> values) {
        return values == null ? List.of() : List.copyOf(new ArrayList<>(values));
    }
}
