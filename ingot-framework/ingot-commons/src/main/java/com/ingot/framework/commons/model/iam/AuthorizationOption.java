package com.ingot.framework.commons.model.iam;

/**
 * <p>分页授权配置候选，固定版本携带参数与操作，委派携带可披露的完整限制。</p>
 * @param id 候选实际标识
 * @param name 可披露名称
 * @param summary 候选上下文摘要；委派为授权依据，诊断操作为所属资源名称
 * @param roleRevisionRef 固定角色版本引用
 * @param parameterDefinitions 该版本的范围参数
 * @param grants 该版本合成的操作授权
 * @param actions 操作的应用与资源展示信息
 * @param delegation 单条委派的完整限制
 * @author jy
 * @since 1.0.0
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record AuthorizationOption(@jakarta.validation.constraints.NotBlank String id, @jakarta.validation.constraints.NotBlank String name, String summary, RoleRevisionRef roleRevisionRef, java.util.List<RoleParameterDefinition> parameterDefinitions, java.util.List<ActionGrant> grants, java.util.List<AuthorizationActionOption> actions, DelegationInput delegation) {
}
