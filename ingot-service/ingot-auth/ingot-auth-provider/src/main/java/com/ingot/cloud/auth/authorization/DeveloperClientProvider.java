package com.ingot.cloud.auth.authorization;

import java.util.List;

import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.DeveloperResources;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;
import org.springframework.stereotype.Component;

/**
 * <p>将 OAuth 客户端管理的全局资源装配到 Auth 的授权 SDK。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
public class DeveloperClientProvider implements ResourceObjectProvider {
    /** 返回共享的精确操作描述。 */
    @Override
    public ResourceDescriptor descriptor() {
        return DeveloperResources.client();
    }

    /** 全局客户端管理不支持配置对象候选。 */
    @Override
    public AuthorizationCandidatePage candidates(ResourceObjectQuery query) {
        return new AuthorizationCandidatePage(List.of(), 0, query.page(), query.pageSize(), false,
                "OAuth 客户端管理仅支持全局范围");
    }

    /** 全局客户端管理不接受对象绑定。 */
    @Override
    public boolean objectsExist(AuthorizationContext context, List<String> ids) {
        return false;
    }
}
