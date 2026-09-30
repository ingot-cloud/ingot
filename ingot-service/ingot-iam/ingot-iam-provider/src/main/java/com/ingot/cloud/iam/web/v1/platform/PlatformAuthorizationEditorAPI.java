package com.ingot.cloud.iam.web.v1.platform;

import java.util.List;
import com.ingot.cloud.iam.assignment.PlatformAuthorizationEditor;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

/**
 * <p>平台配置及只读诊断专用分页候选，不依赖通用 CRUD 列表权限。</p>
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/platform")
@RequiredArgsConstructor
public class PlatformAuthorizationEditorAPI implements RShortcuts {
    private final PlatformAuthorizationEditor editor;
    /**
     * 查询角色分配候选。
     * @param kind 类型
     * @param delegationGrantId 授权依据
     * @param revisionId 固定版本
     * @param parameterKey 范围参数
     * @param actionId 操作
     * @param keyword 搜索词
     * @param ids 已选回显
     * @param page 页码
     * @param pageSize 页大小
     * @return 可配置候选
     */
    @GetMapping("/assignments/candidates")
    @Operation(summary="角色分配候选")
    public R<AuthorizationCandidatePage> assignments(@RequestParam AuthorizationCandidateKind kind,
            @RequestParam(required=false) String delegationGrantId, @RequestParam(required=false) String revisionId,
            @RequestParam(required=false) String parameterKey, @RequestParam(required=false) String actionId,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) List<String> ids,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
        return ok(editor.assignments(kind, delegationGrantId, revisionId, parameterKey, actionId, null,
                keyword, ids, page, pageSize));
    }

    /**
     * 查询授权角色树当前层，角色只展示包含可分配版本的节点。
     * @param delegationGrantId 当前单条授权依据；直接分配时可空
     * @param roleId 指定角色查询版本；可空
     * @param keyword 角色名称搜索词
     * @param ids 当前层少量节点回显
     * @param page 页码
     * @param pageSize 页大小
     * @return 当前层最小角色树分页
     */
    @GetMapping("/assignments/role-candidates")
    @Operation(summary = "角色分配两级树候选")
    public R<AuthorizationRoleCandidatePage> roleCandidates(
            @RequestParam(required = false) String delegationGrantId,
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> ids,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ok(editor.roleCandidates(delegationGrantId, roleId, keyword, ids, page, pageSize));
    }
    /**
     * 查询委派编辑候选。
     * @param kind 类型
     * @param actionId 范围操作
     * @param keyword 搜索词
     * @param ids 已选回显
     * @param page 页码
     * @param pageSize 页大小
     * @return 候选
     */
    @GetMapping("/delegations/candidates")
    @Operation(summary="委派编辑候选")
    public R<AuthorizationCandidatePage> delegations(@RequestParam AuthorizationCandidateKind kind,
            @RequestParam(required=false) String actionId, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) List<String> ids, @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int pageSize) {
        return ok(editor.delegations(kind, actionId, keyword, ids, page, pageSize));
    }
    /**
     * 查询只读诊断候选。
     * @param kind 类型
     * @param actionId 操作
     * @param applicationId 应用
     * @param keyword 搜索词
     * @param ids 已选回显
     * @param page 页码
     * @param pageSize 页大小
     * @return 候选
     */
    @GetMapping("/authorization/diagnose/candidates")
    @Operation(summary="权限诊断候选")
    public R<AuthorizationCandidatePage> diagnose(@RequestParam AuthorizationCandidateKind kind,
            @RequestParam(required=false) String actionId, @RequestParam(required=false) String applicationId,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) List<String> ids,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
        return ok(editor.diagnose(kind, actionId, applicationId, keyword, ids, page, pageSize));
    }
}
