package com.ingot.cloud.iam.infrastructure.persistence;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.alibaba.druid.wall.spi.MySqlWallProvider;
import com.ingot.cloud.iam.domain.assignment.PlatformScopeObjectResource;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.domain.assignment.TenantAssignmentOptionKind;
import com.ingot.cloud.iam.domain.assignment.TenantScopeObjectResource;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.domain.authorization.ObjectScopeClause;
import com.ingot.cloud.iam.domain.shared.AuthorizationCandidateKind;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuditReferenceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentLeaderMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamIdentityMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.RoleWorkspaceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.RoleWorkspaceSql;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceSql;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateSql;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * <p>将 IAM 真实 Mapper 的动态 SQL 交给当前 Druid 防火墙检查，覆盖合法查询分支而不连接数据库。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamSqlWallCompatibilityTest {
    private static final BigInteger ID = BigInteger.TEN;
    private static final List<BigInteger> IDS = List.of(ID, BigInteger.valueOf(20));
    private static final String MAPPER_PACKAGE = IamAccountMapper.class.getPackageName();
    private static final Configuration CONFIGURATION = new Configuration();
    private static final List<Class<?>> MAPPERS = new ArrayList<>();
    private static final Set<String> COVERED = new java.util.HashSet<>();
    private static final MySqlWallProvider WALL = IamSqlWallTestInterceptor.provider();

    @BeforeAll
    static void loadActualMappersAndXml() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        for (var resource : resolver.getResources("classpath*:" + MAPPER_PACKAGE.replace('.', '/') + "/*Mapper.class")) {
            String name = resource.getFilename().replace(".class", "");
            Class<?> mapper = Class.forName(MAPPER_PACKAGE + "." + name);
            if (mapper.isInterface()) {
                MAPPERS.add(mapper);
                CONFIGURATION.addMapper(mapper);
            }
        }
        for (var resource : resolver.getResources("classpath*:mapper/*.xml")) {
            try (var input = resource.getInputStream()) {
                new XMLMapperBuilder(input, CONFIGURATION, resource.toString(), CONFIGURATION.getSqlFragments()).parse();
            }
        }
        assertTrue(MAPPERS.size() >= 50, "Mapper discovery must cover the IAM persistence package");
    }

    @AfterAll
    static void requireAllCustomStatementsCovered() {
        var missing = CONFIGURATION.getMappedStatementNames().stream()
                .filter(id -> id.startsWith(MAPPER_PACKAGE + "."))
                .filter(id -> !COVERED.contains(id)).sorted().toList();
        assertTrue(missing.isEmpty(), () -> "Missing SQL scenarios: " + missing);
    }

    @TestFactory
    Stream<DynamicTest> annotationAndXmlStatements() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> mapper : MAPPERS) {
            for (Method method : mapper.getDeclaredMethods()) {
                String statement = mapper.getName() + "." + method.getName();
                if (!CONFIGURATION.hasStatement(statement) || method.isAnnotationPresent(SelectProvider.class)) continue;
                Map<String, Object> parameters = parameters(method);
                add(tests, statement, "bound", parameters);
                if (parameters.containsKey("domain")) {
                    addVariant(tests, statement, parameters, "domain", AuthorizationDomain.TENANT);
                }
                for (String optional : List.of("visible", "sources", "keyword", "name", "kind", "enabled", "groupIds", "assignmentIds")) {
                    if (parameters.containsKey(optional)) addVariant(tests, statement, parameters, optional, null);
                }
                for (String optional : List.of("visible", "sources")) {
                    if (parameters.containsKey(optional)) addVariant(tests, statement, parameters, optional, List.of());
                }
                if (mapper == AuditReferenceMapper.class && parameters.containsKey("tenantId")) {
                    addVariant(tests, statement, parameters, "tenantId", null);
                }
                if (mapper == IamIdentityMapper.class) addVariant(tests, statement, parameters, "memberId", null);
                if (parameters.containsKey("all")) {
                    addVariant(tests, statement, parameters, "all", true);
                    addVariant(tests, statement, parameters, "ids", List.of());
                }
                if (parameters.containsKey("shared")) addVariant(tests, statement, parameters, "shared", true);
            }
        }
        return tests.stream();
    }

    @TestFactory
    Stream<DynamicTest> providerBranches() {
        List<DynamicTest> tests = new ArrayList<>();
        for (AuthorizationCandidateKind kind : AuthorizationCandidateKind.values()) {
            if (kind == AuthorizationCandidateKind.OBJECT) continue;
            for (List<BigInteger> allowed : java.util.Arrays.asList(null, List.<BigInteger>of(), IDS)) {
                var q = new AuthorizationCandidateSql.Query(kind, ID, ID, ID, null, "%", List.of(), allowed, 0, 20);
                query(tests, AuthorizationCandidateMapper.class, "candidate-" + kind + "-" + allowed, q, "page", "count");
            }
        }
        for (PlatformScopeObjectResource resource : PlatformScopeObjectResource.values()) {
            var q = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, ID, null, null,
                    resource.getValue(), "%", List.of(), null, 0, 20, true, null);
            query(tests, AuthorizationCandidateMapper.class, "object-" + resource, q, "page", "count");
        }
        for (AuthorizationCandidateKind kind : List.of(AuthorizationCandidateKind.MEMBER, AuthorizationCandidateKind.ROLE_REVISION, AuthorizationCandidateKind.OBJECT)) {
            var delegation = new AuthorizationCandidateSql.Query(kind, ID, ID, ID, PlatformScopeObjectResource.MENU.getValue(),
                    "%selected%", IDS, IDS, 0, 20, true, ID, ID, ID, ID);
            query(tests, AuthorizationCandidateMapper.class, "selected-delegation-" + kind, delegation, "page", "count");
            if (kind != AuthorizationCandidateKind.MEMBER) {
                var assignment = new AuthorizationCandidateSql.Query(kind, ID, null, ID, PlatformScopeObjectResource.MENU.getValue(),
                        "%selected%", IDS, IDS, 0, 20, true, ID, null, null, null, ID, "objects", true);
                query(tests, AuthorizationCandidateMapper.class, "selected-assignment-" + kind, assignment, "page", "count");
            }
        }
        for (BigInteger role : java.util.Arrays.asList(null, ID)) {
            for (List<BigInteger> allowed : java.util.Arrays.asList(null, List.<BigInteger>of(), IDS)) {
                for (boolean system : List.of(false, true)) {
                    var q = new AuthorizationCandidateSql.RoleQuery(role, "%", IDS, allowed, 0, 20, ID, system);
                    query(tests, AuthorizationCandidateMapper.class, "role-tree-" + role + "-" + allowed + "-" + system, q, "rolePage", "roleCount");
                }
            }
        }
        for (TenantScopeObjectResource resource : TenantScopeObjectResource.values()) {
            for (List<BigInteger> ids : List.of(List.<BigInteger>of(), IDS)) {
                var q = new TenantScopeCandidateSql.Query(resource, 10, "%", ids, 0, 20, true, null);
                query(tests, TenantScopeCandidateMapper.class, "tenant-object-" + resource + "-" + ids, q, "page", "count");
            }
        }
        for (TenantAssignmentOptionKind kind : TenantAssignmentOptionKind.values()) {
            for (BigInteger source : java.util.Arrays.asList(null, ID)) {
                for (MemberPickerBranch branch : branches()) {
                    var q = new TenantAuthorizationOptionSql.Query(ID, ID, source, kind, "%", IDS, 20, 0, ID, branch);
                    query(tests, TenantAdministrationMapper.class, "tenant-option-" + kind + "-" + source + "-" + branch, q, "options", "optionsCount");
                }
            }
        }
        for (ObjectScope scope : scopes()) {
            for (boolean candidates : List.of(false, true)) {
                for (MemberPickerBranch branch : branches()) {
                    var q = new DepartmentLeaderSql.Query(ID, ID, IDS, scope, null, 20, 0, candidates, ID, branch);
                    query(tests, IamDepartmentLeaderMapper.class, "leaders-" + scopeName(scope) + "-" + candidates + "-" + branch, q, "page", "count", "summaries");
                }
                var searched = new DepartmentLeaderSql.Query(ID, ID, IDS, scope, "%selected%", 20, 0, candidates, ID);
                query(tests, IamDepartmentLeaderMapper.class, "leaders-search-" + scopeName(scope) + "-" + candidates, searched, "page", "count");
            }
        }
        for (SubjectType subject : SubjectType.values()) {
            for (List<BigInteger> visible : java.util.Arrays.asList(null, List.<BigInteger>of(), IDS)) {
                for (BigInteger member : java.util.Arrays.asList(null, ID)) {
                    var platform = new RoleWorkspaceSql.Query(ID, ID, ID, visible, subject, member, "%", 0, 20, visible);
                    query(tests, RoleWorkspaceMapper.class, "platform-workspace-" + subject + "-" + visible + "-" + member,
                            platform, "page", "count", "sources", "sourceCount");
                    var tenant = new TenantRoleWorkspaceSql.Query(ID, ID, member, subject, visible, visible, visible, "%", 0, 20);
                    query(tests, TenantRoleWorkspaceMapper.class, "tenant-workspace-" + subject + "-" + visible + "-" + member,
                            tenant, "page", "count", "sources", "sourceCount");
                }
            }
        }
        return tests.stream();
    }

    @Test
    void securityChecksStillRejectTautologiesAndForbiddenStatements() {
        assertFalse(WALL.checkValid("SELECT id FROM iam_account WHERE 1=1"));
        assertFalse(WALL.checkValid("SELECT id INTO OUTFILE '/tmp/iam-wall-test' FROM iam_account"));
        assertFalse(WALL.checkValid("DROP TABLE iam_account"));
        assertTrue(WALL.checkValid("SELECT id FROM iam_account WHERE enabled=TRUE AND id IN (NULL)"));
    }

    private static void query(List<DynamicTest> tests, Class<?> mapper, String name, Object q, String... methods) {
        for (String method : methods) add(tests, mapper.getName() + "." + method, name, Map.of("q", q));
    }

    private static void addVariant(List<DynamicTest> tests, String statement, Map<String, Object> parameters, String field, Object value) {
        var changed = new HashMap<>(parameters);
        changed.put(field, value);
        add(tests, statement, field + "=" + value, changed);
    }

    private static void add(List<DynamicTest> tests, String statement, String name, Map<String, Object> parameters) {
        COVERED.add(statement);
        tests.add(dynamicTest(statement.substring(MAPPER_PACKAGE.length() + 1) + " / " + name, () -> {
            BoundSql sql = CONFIGURATION.getMappedStatement(statement).getBoundSql(parameters);
            var result = WALL.check(sql.getSql());
            assertTrue(result.getViolations().isEmpty(), () -> result.getViolations().stream().map(v -> v.getErrorCode() + ": " + v.getMessage()).toList() + "\n" + sql.getSql());
            assertFalse(sql.getSql().contains("wall-probe"), "User values must remain bound parameters");
        }));
    }

    private static Map<String, Object> parameters(Method method) {
        Map<String, Object> values = new HashMap<>();
        for (var parameter : method.getParameters()) {
            Param annotation = parameter.getAnnotation(Param.class);
            if (annotation == null) throw new IllegalStateException("Missing @Param: " + method);
            values.put(annotation.value(), sample(parameter.getParameterizedType()));
        }
        return values;
    }

    private static Object sample(Type type) {
        if (type instanceof ParameterizedType generic) {
            if (generic.getRawType() == com.baomidou.mybatisplus.core.conditions.Wrapper.class) {
                return new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>().ne("status", "REMOVED");
            }
            Type item = generic.getActualTypeArguments()[0];
            List<Object> values = List.of(sample(item), sample(item));
            return generic.getRawType() == Set.class ? Set.copyOf(values) : values;
        }
        Class<?> raw = (Class<?>) type;
        if (raw == BigInteger.class) return ID;
        if (raw == String.class) return "%wall-probe%' --%";
        if (raw == int.class || raw == Integer.class) return 20;
        if (raw == long.class || raw == Long.class) return 10L;
        if (raw == boolean.class || raw == Boolean.class) return false;
        if (raw == java.time.LocalDateTime.class) return java.time.LocalDateTime.parse("2026-11-07T00:00:00");
        if (raw == Instant.class) return Instant.parse("2026-11-07T00:00:00Z");
        if (raw.isEnum()) return raw.getEnumConstants()[0];
        if (raw == TenantAuthorizationOptionSql.Query.class) {
            return new TenantAuthorizationOptionSql.Query(ID, ID, ID, TenantAssignmentOptionKind.MEMBER, "%", IDS, 20, 0);
        }
        try {
            if (raw.isRecord()) {
                var components = raw.getRecordComponents();
                Class<?>[] types = java.util.Arrays.stream(components).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new);
                Object[] values = java.util.Arrays.stream(components).map(c -> sample(c.getGenericType())).toArray();
                return raw.getDeclaredConstructor(types).newInstance(values);
            }
            return raw.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Add explicit SQL sample for " + raw, ex);
        }
    }

    private static String scopeName(ObjectScope scope) {
        if (scope.coversAll()) return "all";
        if (scope.coversNone()) return "none";
        return scope.clauses().toString();
    }

    private static List<ObjectScope> scopes() {
        var ids = new ObjectScopeClause(Set.copyOf(IDS), List.of());
        var departments = new ObjectScopeClause(Set.of(), List.of(Set.copyOf(IDS)));
        var intersection = new ObjectScopeClause(Set.copyOf(IDS), List.of(Set.of(ID), Set.of(BigInteger.ONE)));
        var emptyDepartments = new ObjectScopeClause(Set.of(), List.of(Set.of()));
        return List.of(ObjectScope.all(), ObjectScope.none(), ObjectScope.of(List.of(ids)), ObjectScope.of(List.of(departments)),
                ObjectScope.of(List.of(intersection, ids)), ObjectScope.of(List.of(emptyDepartments)));
    }

    private static List<MemberPickerBranch> branches() {
        return java.util.Arrays.asList(null, MemberPickerBranch.all(), MemberPickerBranch.none(),
                MemberPickerBranch.department(10, false), MemberPickerBranch.department(10, true));
    }

}
