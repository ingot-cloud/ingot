package com.ingot.example.ops;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.authorization.FieldPolicyProcessor;
import com.ingot.framework.authorization.ScopeSql;
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

    /**
     * 范围SQL在分页和count之前生效。
     * @param page 页码
     * @param pageSize 大小
     * @param title 原值搜索
     * @return 投影页
     */
    @GetMapping
    public PageResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String title) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE)
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var decision = authorization.require(IncidentProvider.KEY, IncidentProvider.READ);
        if (title != null && !title.isBlank())
            FieldPolicyProcessor.requireOriginalLookup(FieldPolicyProcessor.forAction(decision, IncidentProvider.READ), IncidentProvider.TITLE);
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
    public Map<String, Object> detail(@PathVariable String id) {
        var decision = authorization.require(IncidentProvider.KEY, IncidentProvider.READ);
        var row = incidents.selectById(id);
        if (row == null)
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        authorization.requireTarget(decision, IncidentProvider.READ, target(row));
        return project(decision, row, IncidentProvider.READ);
    }

    /**
     * 写操作fresh求值，事务锁定实体，空值也经过编辑校验。
     * @param id UUID
     * @param submitted 实际提交字段
     */
    @PatchMapping("/{id}")
    @Transactional
    public void patch(@PathVariable String id, @RequestBody Map<String, Object> submitted) {
        var row = incidents.lock(id);
        if (row == null)
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        var decision = authorization.require(IncidentProvider.KEY, IncidentProvider.UPDATE);
        authorization.requireTarget(decision, IncidentProvider.UPDATE, target(row));
        FieldPolicyProcessor.requireWritable(submitted, FieldPolicyProcessor.access(FieldPolicyProcessor.forAction(decision, IncidentProvider.UPDATE), target(row)));
        var update = Wrappers.<Incident>lambdaUpdate().eq(Incident::getId, row.getId());
        if (submitted.containsKey(IncidentProvider.TITLE))
            update.set(Incident::getTitle, text(submitted.get(IncidentProvider.TITLE)));
        if (submitted.containsKey(IncidentProvider.CONTACT))
            update.set(Incident::getContact, text(submitted.get(IncidentProvider.CONTACT)));
        if (!submitted.isEmpty())
            incidents.update(null, update);
    }

    /**
     * 导出与页面使用相同范围和字段投影，示例以逐页拉取输出。
     * @param page 页码
     * @return 投影导出页
     */
    @GetMapping("/export")
    public List<Map<String, Object>> export(@RequestParam(defaultValue = "1") int page) {
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

    private static Map<String, Object> project(AuthorizationDecision decision, Incident row, String action) {
        Map<String, Object> raw = new java.util.LinkedHashMap<>();
        raw.put(IncidentProvider.TITLE, row.getTitle());
        raw.put(IncidentProvider.CONTACT, row.getContact());
        var projected = new java.util.LinkedHashMap<>(FieldPolicyProcessor.project(raw,
                FieldPolicyProcessor.access(FieldPolicyProcessor.forAction(decision, action), target(row)), Map.of()));
        projected.put("id", row.getId());
        return java.util.Collections.unmodifiableMap(projected);
    }

    private static String text(Object value) {
        if (value == null)
            return null;
        if (!(value instanceof String))
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        return (String) value;
    }

}
