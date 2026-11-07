package com.ingot.cloud.iam.application.diagnose;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.application.audit.model.AuditEntry;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.cloud.iam.domain.audit.AuditReferenceType;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.cloud.iam.domain.assignment.SubjectType;

/**
 * <p>从一页审计中收集需要解析名称的对象，只识别白名单字段的已知结构，无法解析的值跳过。</p>
 *
 * @author jy
 * @since 1.0.0
 */
final class AuditReferenceCollector {
    /** 应用开通审计的目标 ID 是租户 ID。 */
    static final String TARGET_ENTITLEMENT = "entitlement";
    /** 应用人群审计的目标 ID 是应用 ID。 */
    static final String TARGET_AUDIENCE = "audience";
    /** 角色版本应用任务审计的目标类型。 */
    static final String TARGET_ROLE_APPLICATION = "ROLE_VERSION_APPLICATION";

    private static final String ADDED_MEMBERS = "addedMemberIds";
    private static final String REMOVED_MEMBERS = "removedMemberIds";
    private static final String PRIMARY_MEMBER = "primaryMemberId";
    private static final String SUBJECT_TYPE = "type";
    private static final String SUBJECT_ID = "id";
    private static final String SELECTION_MEMBERS = "members";
    private static final String SELECTION_DEPARTMENTS = "departments";
    private static final String ADMINISTRATOR = "administratorMemberId";
    private static final String ALLOWED_ROLES = "allowedRoleIds";
    private static final String CEILINGS = "resourceScopeCeilings";
    private static final String SCOPE_BINDINGS = "scopeBindings";
    private static final String BINDING_KIND = "kind";
    private static final String BINDING_IDS = "ids";
    private static final Pattern ID_PATTERN = Pattern.compile("[1-9][0-9]{0,19}");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, AuditReferenceType> TARGETS = Map.of(
            AuditReferenceType.MEMBER.getValue(), AuditReferenceType.MEMBER,
            AuditReferenceType.DEPARTMENT.getValue(), AuditReferenceType.DEPARTMENT,
            AuditReferenceType.GROUP.getValue(), AuditReferenceType.GROUP,
            AuditReferenceType.ROLE.getValue(), AuditReferenceType.ROLE,
            AuditReferenceType.ASSIGNMENT.getValue(), AuditReferenceType.ASSIGNMENT,
            AuditReferenceType.DELEGATION.getValue(), AuditReferenceType.DELEGATION,
            AuditReferenceType.TENANT.getValue(), AuditReferenceType.TENANT,
            AuditReferenceType.ACCOUNT.getValue(), AuditReferenceType.ACCOUNT,
            AuditReferenceType.APPLICATION.getValue(), AuditReferenceType.APPLICATION,
            TARGET_ENTITLEMENT, AuditReferenceType.TENANT);

    private final Map<AuditReferenceType, Set<String>> ids = new EnumMap<>(AuditReferenceType.class);
    private final Map<String, String> snapshots = new LinkedHashMap<>();

    /**
     * 审计目标对应的引用类型；单例或任务类目标返回 {@code null}。
     *
     * @param targetType 审计 target_type
     * @return 引用类型或 {@code null}
     */
    static AuditReferenceType targetType(String targetType) {
        if (TARGET_AUDIENCE.equals(targetType)) {
            return AuditReferenceType.APPLICATION;
        }
        if (TARGET_ROLE_APPLICATION.equals(targetType)) {
            return AuditReferenceType.ROLE_APPLICATION_TASK;
        }
        return TARGETS.get(targetType);
    }

    /**
     * 收集一条审计；按页内顺序调用时快照取最新一条。
     *
     * @param entry 已解码的审计
     */
    void collect(AuditEntry entry) {
        add(AuditReferenceType.MEMBER, entry.actor().memberId());
        AuditReferenceType target = targetType(entry.target().resource());
        if (target != null && add(target, entry.target().id())) {
            String snapshot = entry.changeType() == AuditChangeType.REMOVE || entry.after().get(AuditField.NAME) == null
                    ? entry.before().get(AuditField.NAME) : entry.after().get(AuditField.NAME);
            if (snapshot != null && !snapshot.isBlank()) {
                snapshots.putIfAbsent(target.key(entry.target().id()), snapshot);
            }
        }
        add(AuditReferenceType.ASSIGNMENT, entry.assignmentId());
        add(AuditReferenceType.DELEGATION, entry.delegationId());
        values(entry.before());
        values(entry.after());
    }

    /**
     * 追加由一级对象展开出的引用。
     *
     * @param type 类型
     * @param id ID，非数字忽略
     * @return 是否为合法 ID
     */
    boolean add(AuditReferenceType type, String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            return false;
        }
        ids.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(id);
        return true;
    }

    /**
     * 某类型已收集的 ID。
     *
     * @param type 类型
     * @return ID 集合，可能为空
     */
    Set<String> ids(AuditReferenceType type) {
        return ids.getOrDefault(type, Set.of());
    }

    /**
     * 目标对象的快照名称，键为 {@code type:id}。
     *
     * @return 快照
     */
    Map<String, String> snapshots() {
        return snapshots;
    }

    private void values(Map<AuditField, String> values) {
        values.forEach((field, value) -> {
            if (value == null) {
                return;
            }
            switch (field) {
                case ROLE -> add(AuditReferenceType.ROLE, value);
                case ROLE_REVISION -> add(AuditReferenceType.ROLE_REVISION, value);
                case OWNER_MEMBER -> add(AuditReferenceType.MEMBER, value);
                case DEPARTMENT_LEADERS -> leaders(value);
                case RECIPIENT_SELECTION -> recipients(tree(value));
                case SCOPE -> scope(tree(value));
                default -> {
                }
            }
        });
    }

    private void leaders(String value) {
        if (add(AuditReferenceType.MEMBER, value)) {
            return;
        }
        JsonNode node = tree(value);
        if (node == null) {
            return;
        }
        texts(node.get(ADDED_MEMBERS), AuditReferenceType.MEMBER);
        texts(node.get(REMOVED_MEMBERS), AuditReferenceType.MEMBER);
        text(node.get(PRIMARY_MEMBER), AuditReferenceType.MEMBER);
    }

    private void recipients(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            node.forEach(this::recipients);
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode type = node.get(SUBJECT_TYPE);
        if (type != null && type.isTextual()) {
            AuditReferenceType target = SubjectType.GROUP.getValue().equals(type.asText())
                    ? AuditReferenceType.GROUP : AuditReferenceType.MEMBER;
            text(node.get(SUBJECT_ID), target);
            return;
        }
        texts(node.get(SELECTION_MEMBERS), AuditReferenceType.MEMBER);
        JsonNode departments = node.get(SELECTION_DEPARTMENTS);
        if (departments != null && departments.isArray()) {
            departments.forEach(item -> text(item.get(SUBJECT_ID), AuditReferenceType.DEPARTMENT));
        }
    }

    private void scope(JsonNode node) {
        if (node == null || !node.isObject()) {
            return;
        }
        if (node.has(ADMINISTRATOR) || node.has(CEILINGS)) {
            text(node.get(ADMINISTRATOR), AuditReferenceType.MEMBER);
            texts(node.get(ALLOWED_ROLES), AuditReferenceType.ROLE);
            JsonNode ceilings = node.get(CEILINGS);
            if (ceilings != null && ceilings.isArray()) {
                ceilings.forEach(ceiling -> bindings(ceiling.get(SCOPE_BINDINGS)));
            }
            return;
        }
        JsonNode departments = node.get(SELECTION_DEPARTMENTS);
        if (departments != null && departments.isObject()) {
            primaryFlags(departments);
            return;
        }
        if (primaryFlags(node)) {
            return;
        }
        bindings(node);
    }

    /** 成员任职 {@code {部门ID: 是否主部门}}；值不全为布尔时返回 false。 */
    private boolean primaryFlags(JsonNode node) {
        if (node.isEmpty()) {
            return false;
        }
        for (JsonNode value : node) {
            if (!value.isBoolean()) {
                return false;
            }
        }
        node.fieldNames().forEachRemaining(id -> add(AuditReferenceType.DEPARTMENT, id));
        return true;
    }

    private void bindings(JsonNode node) {
        if (node == null || !node.isObject()) {
            return;
        }
        node.forEach(binding -> {
            JsonNode kind = binding.get(BINDING_KIND);
            if (kind != null && ScopeBindingKind.DEPARTMENTS.getValue().equals(kind.asText())) {
                texts(binding.get(BINDING_IDS), AuditReferenceType.DEPARTMENT);
            }
        });
    }

    private void texts(JsonNode node, AuditReferenceType type) {
        if (node != null && node.isArray()) {
            node.forEach(item -> text(item, type));
        }
    }

    private void text(JsonNode node, AuditReferenceType type) {
        if (node != null && (node.isTextual() || node.isIntegralNumber())) {
            add(type, node.asText());
        }
    }

    private static JsonNode tree(String value) {
        if (value.isEmpty() || (value.charAt(0) != '{' && value.charAt(0) != '[')) {
            return null;
        }
        try {
            return MAPPER.readTree(value);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
