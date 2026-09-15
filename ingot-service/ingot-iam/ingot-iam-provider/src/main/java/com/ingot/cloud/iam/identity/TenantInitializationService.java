package com.ingot.cloud.iam.identity;

import lombok.RequiredArgsConstructor;

import java.util.List;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.TenantCreateInput;
import com.ingot.framework.commons.model.iam.TenantPreviewResult;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import org.springframework.stereotype.Service;

/**
 * <p>编排组织创建预览与提交：先校验平台身份和 ACTION，再由服务器目录生成计划。</p>
 *
 * <p>HTTP 只接收 {@link TenantCreateInput}；治理版本、默认策略和基础应用不接受客户端指定。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantInitializationService {
    private final CurrentIdentityService current;
    private final IamActionAuthorizer authorizer;
    private final InitializationCatalog catalog;
    private final TenantInitializer initializer;
    private final InitializationIdAllocator ids;



    /**
     * 预览将创建的最小实体，不分配 ID、不写入。
     *
     * @param input 组织资料与所有者账号
     * @return 目录解析结果；目录不完整时 valid=false
     */
    public Preview<TenantPreviewResult> preview(TenantCreateInput input) {
        ActiveIdentity actor = current.requireDomain(AuthorizationDomain.PLATFORM);
        authorizer.require(actor.context(), IamAction.PLATFORM_TENANT_PREVIEW);
        try {
            TenantPreviewResult result = catalog.preview(input);
            return new Preview<>(actor.memberVersion(), true, List.of(), List.of(),
                    new ImpactSummary(1L, 1L, 0L, false), result);
        } catch (BizException exception) {
            IamReasonCode code = reason(exception);
            if (code == IamReasonCode.AUTHORIZATION_UNAVAILABLE) {
                throw exception;
            }
            return new Preview<>(actor.memberVersion(), false,
                    List.of(new ValidationIssue("ownerAccountId", code, code.getText())),
                    List.of(), new ImpactSummary(null, null, null, false), null);
        }
    }

    /**
     * 校验创建 ACTION 后原子初始化组织。
     *
     * @param input 组织资料与所有者账号
     * @return 新组织 ID 与版本
     */
    public CreatedResource create(TenantCreateInput input) {
        ActiveIdentity actor = current.requireDomain(AuthorizationDomain.PLATFORM);
        authorizer.require(actor.context(), IamAction.PLATFORM_TENANT_CREATE);
        return initializer.initialize(actor.context(), catalog.plan(input, ids));
    }

    private static IamReasonCode reason(BizException exception) {
        for (IamReasonCode code : IamReasonCode.values()) {
            if (code.getCode().equals(exception.getCode())) {
                return code;
            }
        }
        return IamReasonCode.INVALID_ARGUMENT;
    }
}
