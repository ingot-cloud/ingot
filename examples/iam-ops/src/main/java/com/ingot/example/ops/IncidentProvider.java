package com.ingot.example.ops;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.AuthorizationOption;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>独立iam-ops资源，候选、存在性和实际接口共用UUID。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class IncidentProvider implements ResourceObjectProvider {

    /**
     * 完整资源键。
     */
    public static final String APPLICATION = "iam-ops";
    /** 资源编码。 */
    public static final String RESOURCE = "incident";
    /** 完整资源身份。 */
    public static final ResourceKey KEY = new ResourceKey(AuthorizationDomain.PLATFORM, APPLICATION, RESOURCE);

    /**
     * 读取工单。
     */
    public static final String READ = "iam-ops:incident:read";

    /**
     * 更新工单。
     */
    public static final String UPDATE = "iam-ops:incident:update";

    /**
     * 导出工单。
     */
    public static final String EXPORT = "iam-ops:incident:export";

    /**
     * 标题字段。
     */
    public static final String TITLE = "title";

    /**
     * 联系字段。
     */
    public static final String CONTACT = "contact";

    private static final int MAX_PAGE_SIZE = 100;

    private final IncidentMapper incidents;

    @Override
    public ResourceDescriptor descriptor() {
        return new ResourceDescriptor(KEY,
                List.of(new ActionDescriptor(READ, ExecutionMode.READ_ONLY),
                        new ActionDescriptor(UPDATE, ExecutionMode.MUTATING),
                        new ActionDescriptor(EXPORT, ExecutionMode.READ_ONLY)),
                List.of(ScopeKind.ALL, ScopeKind.SELF, ScopeKind.OBJECT_SET),
                List.of(new FieldCapability(TITLE, "标题", List.of(FieldVisibility.values()), true, true, com.ingot.framework.commons.model.iam.MaskSpec.ALL),
                        new FieldCapability(CONTACT, "联系方式", List.of(FieldVisibility.values()), true, false, com.ingot.framework.commons.model.iam.MaskSpec.EMAIL)),
                Map.of(TITLE, new FieldAccess(FieldVisibility.FULL, true), CONTACT,
                        new FieldAccess(FieldVisibility.MASKED, false)),
                READ, false);
    }

    @Override
    public AuthorizationCandidatePage candidates(ResourceObjectQuery input) {
        if (input.page() < 1 || input.pageSize() < 1 || input.pageSize() > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("分页不合法");
        var query = Wrappers.<Incident>lambdaQuery()
            .select(Incident::getId, Incident::getTitle)
            .like(input.keyword() != null && !input.keyword().isBlank(), Incident::getTitle, input.keyword())
            .in(!input.ids().isEmpty(), Incident::getId, input.ids())
            .orderByDesc(Incident::getId);
        if (input.allowedIds() != null) {
            if (input.allowedIds().isEmpty())
                return new AuthorizationCandidatePage(List.of(), 0, input.page(), input.pageSize(), true, null);
            query.in(Incident::getId, input.allowedIds());
        }
        var result = incidents.selectPage(new Page<>(input.page(), input.pageSize()), query);
        return new AuthorizationCandidatePage(result.getRecords()
            .stream()
            .map(row -> new AuthorizationOption(row.getId(), row.getTitle(), null, null, null, null, null, null))
            .toList(), result.getTotal(), input.page(), input.pageSize(), true, null);
    }

    @Override
    public boolean objectsExist(AuthorizationContext context, List<String> ids) {
        if (context.domain() != AuthorizationDomain.PLATFORM || ids.isEmpty())
            return false;
        return incidents.selectCount(Wrappers.<Incident>lambdaQuery()
            .in(Incident::getId, ids.stream().distinct().toList())) == ids.stream().distinct().count();
    }

}
