package com.ingot.example.ops;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.authorization.FieldPolicyProcessor;
import com.ingot.framework.authorization.ScopeSql;
import com.ingot.framework.authorization.field.*;
import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>可编译的独立服务接入示例，不安装到现有生产菜单或业务表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/ops/incidents")
public class IncidentAPI {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuthorizationAccess authorization;

    private final IncidentMapper incidents;
    private final FieldProjectionEngine projection;
    private final FieldBindingRegistry bindings;
    private final FieldWriteExecutor writes;

    /**
     * 范围SQL在分页和count之前生效。
     * @param page 页码
     * @param pageSize 大小
     * @param title 原值搜索
     * @return 投影页
     */
    @GetMapping
    @FieldControl(domain = AuthorizationDomain.PLATFORM, applicationCode = IncidentProvider.APPLICATION, resourceCode = IncidentProvider.RESOURCE, action = IncidentProvider.READ, valueType = IncidentViews.Row.class)
    @FieldControl(domain = AuthorizationDomain.PLATFORM, applicationCode = IncidentProvider.APPLICATION, resourceCode = IncidentProvider.RESOURCE, action = IncidentProvider.READ, valueType = IncidentViews.Filter.class, use = FieldUse.FILTER)
    public PageResponse<ObjectNode> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String title) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE)
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var decision = authorization.require(IncidentProvider.KEY, IncidentProvider.READ);
        if (title != null && !title.isBlank())
            FieldPolicyProcessor.requireOriginalLookup(FieldPolicyProcessor.forAction(decision, IncidentProvider.READ), IncidentProvider.TITLE, decision.actions().get(IncidentProvider.READ).scope());
        var query = Wrappers.<Incident>lambdaQuery().like(title != null && !title.isBlank(), Incident::getTitle, title);
        ScopeSql.restrict(query, decision, IncidentProvider.READ, Incident::getId, Incident::getOwnerMemberId, null,
                null);
        var result = incidents.selectPage(new Page<>(page, pageSize), query);
        return new PageResponse<>(result.getRecords().stream().map(row -> project(decision, row, IncidentProvider.READ)).toList(),
                result.getTotal(), page, pageSize);
    }

    /**
     * 详情通过真实归属检查。
     * @param id UUID
     * @return 投影字段
     */
    @GetMapping("/{id}")
    @FieldControl(domain = AuthorizationDomain.PLATFORM, applicationCode = IncidentProvider.APPLICATION, resourceCode = IncidentProvider.RESOURCE, action = IncidentProvider.READ, valueType = IncidentViews.Detail.class)
    public ObjectNode detail(@PathVariable String id) {
        var decision = authorization.require(IncidentProvider.KEY, IncidentProvider.READ);
        var row = incidents.selectById(id);
        if (row == null)
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        authorization.requireTarget(decision, IncidentProvider.READ, target(row));
        return project(decision, row, IncidentProvider.READ, new IncidentViews.Detail(row.getId(), row.getTitle(), row.getContact()));
    }

    /**
     * 写操作fresh求值，事务锁定实体，空值也经过编辑校验。
     * @param id UUID
     * @param submitted 实际提交字段
     */
    @PatchMapping("/{id}")
    @FieldControl(domain = AuthorizationDomain.PLATFORM, applicationCode = IncidentProvider.APPLICATION, resourceCode = IncidentProvider.RESOURCE, action = IncidentProvider.UPDATE, valueType = IncidentViews.Patch.class, use = FieldUse.WRITE)
    @Transactional
    public void patch(@PathVariable String id, @RequestBody JsonNode submitted) {
        var row = incidents.lock(id);
        if (row == null)
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        var actual = FieldInputCollector.collect(submitted, bindings.require(IncidentViews.Patch.class, IncidentProvider.KEY, FieldUse.WRITE))
                .getOrDefault(IncidentProvider.KEY, Map.of());
        writes.require(IncidentProvider.KEY, IncidentProvider.UPDATE, target(row), actual);
        var update = Wrappers.<Incident>lambdaUpdate().eq(Incident::getId, row.getId());
        if (actual.containsKey(IncidentProvider.TITLE))
            update.set(Incident::getTitle, text(actual.get(IncidentProvider.TITLE)));
        if (actual.containsKey(IncidentProvider.CONTACT))
            update.set(Incident::getContact, text(actual.get(IncidentProvider.CONTACT)));
        if (!actual.isEmpty())
            incidents.update(null, update);
    }

    /**
     * 导出与页面使用相同范围和字段投影，示例以逐页拉取输出。
     * @param page 页码
     * @return 投影导出页
     */
    @GetMapping("/export")
    @FieldControl(domain = AuthorizationDomain.PLATFORM, applicationCode = IncidentProvider.APPLICATION, resourceCode = IncidentProvider.RESOURCE, action = IncidentProvider.EXPORT, valueType = IncidentViews.Row.class)
    public List<ObjectNode> export(@RequestParam(defaultValue = "1") int page) {
        if (page < 1)
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var decision = authorization.require(IncidentProvider.KEY, IncidentProvider.EXPORT);
        var query = Wrappers.<Incident>lambdaQuery();
        ScopeSql.restrict(query, decision, IncidentProvider.EXPORT, Incident::getId, Incident::getOwnerMemberId, null,
                null);
        return incidents.selectPage(new Page<>(page, MAX_PAGE_SIZE), query)
            .getRecords()
            .stream()
            .map(row -> project(decision, row, IncidentProvider.EXPORT))
            .toList();
    }

    private static ScopeTarget target(Incident row) {
        return new ScopeTarget(row.getId(), row.getOwnerMemberId(), null, List.of());
    }

    private ObjectNode project(AuthorizationDecision decision, Incident row, String action) {
        return project(decision, row, action, new IncidentViews.Row(row.getId(), row.getTitle(), row.getContact()));
    }

    private ObjectNode project(AuthorizationDecision decision, Incident row, String action, Object dto) {
        var policy = FieldPolicyProcessor.forAction(decision, action);
        return projection.project(dto, IncidentProvider.KEY, new FieldReadSnapshot(
                Map.of(IncidentProvider.KEY, FieldPolicyProcessor.access(policy, target(row))), Map.of(IncidentProvider.KEY, policy.masks())));
    }

    /** 提供全局能力；具体对象仍在业务事务中使用最新权限。 */
    @GetMapping("/context")
    public ResourceFieldContext context() {
        var read = authorization.preview(IncidentProvider.KEY, IncidentProvider.READ);
        var policy = FieldPolicyProcessor.forAction(read, IncidentProvider.READ);
        Map<String, Map<String, FieldOperations>> operations = new java.util.LinkedHashMap<>();
        operations.put(IncidentProvider.READ, policy.operations());
        try { operations.put(IncidentProvider.UPDATE, FieldPolicyProcessor.forAction(authorization.preview(IncidentProvider.KEY, IncidentProvider.UPDATE), IncidentProvider.UPDATE).operations()); }
        catch (com.ingot.framework.authorization.SdkAuthorizationException denied) {
            if (!IamReasonCode.ACTION_DENIED.getCode().equals(denied.getCode())) throw denied;
            operations.put(IncidentProvider.UPDATE, Map.of());
        }
        Map<String, FieldVisibility> visible = new java.util.LinkedHashMap<>();
        policy.ceilings().forEach((key, value) -> visible.put(key, value.visibility()));
        return new ResourceFieldContext(visible, operations, policy.masks());
    }

    private static String text(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        return value.textValue();
    }
}
