package com.ingot.cloud.iam.role;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.ActionOrigin;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleDelta;
import com.ingot.framework.commons.model.iam.RoleDeltaOperation;
import com.ingot.framework.commons.model.iam.RoleOrigin;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.UpgradeChange;
import com.ingot.framework.commons.model.iam.UpgradeChangeKind;
import com.ingot.framework.commons.model.iam.UpgradeConflict;
import com.ingot.framework.commons.model.iam.UpgradeResolution;
import com.ingot.framework.commons.model.iam.UpgradeResolutionChoice;

/**
 * <p>按固定顺序把基础授权与租户差异合成完整定义，不把合成结果当作持久化来源。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class RoleSynthesis {
    private RoleSynthesis() {
    }

    /**
     * 读取固定基础后依次应用 REMOVE、REPLACE_SCOPE、ADD。
     *
     * @param base 完整基础授权
     * @param deltas 租户差异；完整版本传空集合
     * @return 合成授权与来源
     */
    public static Result synthesize(List<ActionGrant> base, List<RoleDelta> deltas) {
        Map<String, List<ScopeExpression>> grants = new LinkedHashMap<>();
        Map<String, RoleOrigin> origins = new LinkedHashMap<>();
        for (ActionGrant grant : base == null ? List.<ActionGrant>of() : base) {
            grants.put(grant.actionId(), copy(grant.scopes()));
            origins.put(grant.actionId(), RoleOrigin.BASE);
        }
        List<RoleDelta> ordered = new ArrayList<>();
        if (deltas != null) {
            deltas.stream().filter(delta -> delta.operation() == RoleDeltaOperation.REMOVE).forEach(ordered::add);
            deltas.stream().filter(delta -> delta.operation() == RoleDeltaOperation.REPLACE_SCOPE).forEach(ordered::add);
            deltas.stream().filter(delta -> delta.operation() == RoleDeltaOperation.ADD).forEach(ordered::add);
        }
        for (RoleDelta delta : ordered) {
            apply(grants, origins, delta);
        }
        List<ActionGrant> result = new ArrayList<>();
        List<ActionOrigin> originList = new ArrayList<>();
        grants.forEach((actionId, scopes) -> result.add(new ActionGrant(actionId, copy(scopes))));
        origins.forEach((actionId, origin) -> originList.add(new ActionOrigin(actionId, origin)));
        return new Result(List.copyOf(result), List.copyOf(originList));
    }

    /**
     * 比较新旧基础并叠加当前差异，找出必须显式处置的冲突。
     *
     * @param oldBase 旧基础授权
     * @param newBase 新基础授权
     * @param deltas 当前租户差异
     * @param resolutions 已选处置，可空
     * @return 变化、冲突与可合成时的新差异
     */
    public static UpgradePlan upgrade(List<ActionGrant> oldBase, List<ActionGrant> newBase,
                                      List<RoleDelta> deltas, List<UpgradeResolution> resolutions) {
        Map<String, List<ScopeExpression>> oldGrants = index(oldBase);
        Map<String, List<ScopeExpression>> nextGrants = index(newBase);
        List<UpgradeChange> changes = new ArrayList<>();
        for (String actionId : union(oldGrants.keySet(), nextGrants.keySet())) {
            boolean had = oldGrants.containsKey(actionId);
            boolean has = nextGrants.containsKey(actionId);
            if (!had) {
                changes.add(change(actionId, UpgradeChangeKind.ADDED, List.of(), nextGrants.get(actionId)));
            } else if (!has) {
                changes.add(change(actionId, UpgradeChangeKind.REMOVED, oldGrants.get(actionId), List.of()));
            } else if (!Objects.equals(oldGrants.get(actionId), nextGrants.get(actionId))) {
                changes.add(change(actionId, UpgradeChangeKind.SCOPE_CHANGED, oldGrants.get(actionId),
                        nextGrants.get(actionId)));
            }
        }
        Map<String, RoleDelta> current = new LinkedHashMap<>();
        for (RoleDelta delta : deltas == null ? List.<RoleDelta>of() : deltas) {
            current.put(delta.actionId(), delta);
        }
        Map<String, UpgradeResolution> chosen = new LinkedHashMap<>();
        for (UpgradeResolution resolution : resolutions == null ? List.<UpgradeResolution>of() : resolutions) {
            chosen.put(resolution.key(), resolution);
        }
        List<UpgradeConflict> conflicts = new ArrayList<>();
        List<RoleDelta> nextDeltas = new ArrayList<>();
        for (UpgradeChange change : changes) {
            RoleDelta delta = current.get(change.actionId());
            if (delta == null) {
                continue;
            }
            String key = change.key();
            UpgradeResolution resolution = chosen.get(key);
            if (resolution == null) {
                conflicts.add(new UpgradeConflict(key, change.actionId(), IamReasonCode.POLICY_CONFLICT,
                        "共享基础变化与当前差异冲突，必须显式选择"));
                continue;
            }
            switch (resolution.choice()) {
                case ACCEPT_BASE -> current.remove(change.actionId());
                case KEEP_DELTA -> {
                    if (!keepValid(delta, nextGrants)) {
                        conflicts.add(new UpgradeConflict(key, change.actionId(), IamReasonCode.POLICY_CONFLICT,
                                "保留差异后无法在新基础上合成"));
                    }
                }
                case REPLACE_SCOPE -> {
                    if (resolution.scopes() == null) {
                        conflicts.add(new UpgradeConflict(key, change.actionId(), IamReasonCode.INVALID_ARGUMENT,
                                "替换范围必须携带范围集合"));
                    } else {
                        current.put(change.actionId(), new RoleDelta(change.actionId(),
                                RoleDeltaOperation.REPLACE_SCOPE, resolution.scopes()));
                    }
                }
            }
        }
        if (conflicts.isEmpty()) {
            nextDeltas.addAll(current.values());
            synthesize(newBase, nextDeltas);
        }
        return new UpgradePlan(List.copyOf(changes), List.copyOf(conflicts), List.copyOf(nextDeltas));
    }

    private static void apply(Map<String, List<ScopeExpression>> grants, Map<String, RoleOrigin> origins,
                              RoleDelta delta) {
        boolean exists = grants.containsKey(delta.actionId());
        switch (delta.operation()) {
            case REMOVE -> {
                if (!exists) {
                    throw new BizException(IamReasonCode.POLICY_CONFLICT);
                }
                grants.remove(delta.actionId());
                origins.put(delta.actionId(), RoleOrigin.REMOVED);
            }
            case REPLACE_SCOPE -> {
                if (!exists) {
                    throw new BizException(IamReasonCode.POLICY_CONFLICT);
                }
                grants.put(delta.actionId(), copy(delta.scopes()));
                origins.put(delta.actionId(), RoleOrigin.REPLACED);
            }
            case ADD -> {
                if (exists) {
                    throw new BizException(IamReasonCode.POLICY_CONFLICT);
                }
                grants.put(delta.actionId(), copy(delta.scopes()));
                origins.put(delta.actionId(), RoleOrigin.ADDED);
            }
        }
    }

    private static boolean keepValid(RoleDelta delta, Map<String, List<ScopeExpression>> newBase) {
        return switch (delta.operation()) {
            case ADD -> !newBase.containsKey(delta.actionId());
            case REMOVE, REPLACE_SCOPE -> newBase.containsKey(delta.actionId());
        };
    }

    private static Map<String, List<ScopeExpression>> index(List<ActionGrant> grants) {
        Map<String, List<ScopeExpression>> result = new LinkedHashMap<>();
        for (ActionGrant grant : grants == null ? List.<ActionGrant>of() : grants) {
            result.put(grant.actionId(), copy(grant.scopes()));
        }
        return result;
    }

    private static UpgradeChange change(String actionId, UpgradeChangeKind kind, List<ScopeExpression> oldScopes,
                                        List<ScopeExpression> newScopes) {
        return new UpgradeChange("action:" + actionId, actionId, kind, copy(oldScopes), copy(newScopes));
    }

    private static List<String> union(java.util.Set<String> left, java.util.Set<String> right) {
        LinkedHashMap<String, Boolean> keys = new LinkedHashMap<>();
        left.forEach(key -> keys.put(key, Boolean.TRUE));
        right.forEach(key -> keys.put(key, Boolean.TRUE));
        return List.copyOf(keys.keySet());
    }

    private static List<ScopeExpression> copy(List<ScopeExpression> scopes) {
        return scopes == null ? List.of() : List.copyOf(scopes);
    }

    /**
     * <p>保存合成后的授权与来源，移除项只出现在来源中。</p>
     *
     * @param grants 仍有效的操作范围
     * @param origins 含 REMOVED 的来源
     * @author jy
     * @since 1.0.0
     */
    public record Result(List<ActionGrant> grants, List<ActionOrigin> origins) {
    }

    /**
     * <p>保存升级三方比较结果及可提交的新差异。</p>
     *
     * @param changes 基础变化
     * @param conflicts 未解决冲突
     * @param nextDeltas 冲突全部解决后的新差异
     * @author jy
     * @since 1.0.0
     */
    public record UpgradePlan(List<UpgradeChange> changes, List<UpgradeConflict> conflicts,
                              List<RoleDelta> nextDeltas) {
    }
}
