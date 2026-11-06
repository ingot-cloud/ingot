package com.ingot.framework.commons.model.iam;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;

/**
 * <p>开发者平台 OAuth 客户端的精确目录与可信执行描述。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DeveloperResources {
    /** 开发者平台应用编码。 */
    public static final String APPLICATION = "platform-developer";
    /** OAuth 客户端资源键。 */
    public static final ResourceKey CLIENT = new ResourceKey(AuthorizationDomain.PLATFORM, APPLICATION, "oauth-client");
    /** 查看客户端列表。 */
    public static final String CLIENT_READ_LIST = APPLICATION + ":oauth-client:read-list";
    /** 查看客户端详情。 */
    public static final String CLIENT_READ_DETAIL = APPLICATION + ":oauth-client:read-detail";
    /** 创建客户端。 */
    public static final String CLIENT_CREATE = APPLICATION + ":oauth-client:create";
    /** 编辑客户端。 */
    public static final String CLIENT_UPDATE = APPLICATION + ":oauth-client:update";
    /** 删除客户端。 */
    public static final String CLIENT_DELETE = APPLICATION + ":oauth-client:delete";
    /** 重置客户端密钥。 */
    public static final String CLIENT_RESET_SECRET = APPLICATION + ":oauth-client:reset-secret";

    private DeveloperResources() {
    }

    /** 客户端管理仅允许 ALL，读取和写入按各自执行模式求值。 */
    public static ResourceDescriptor client() {
        return new ResourceDescriptor(CLIENT, List.of(
                new ActionDescriptor(CLIENT_READ_LIST, ExecutionMode.READ_ONLY),
                new ActionDescriptor(CLIENT_READ_DETAIL, ExecutionMode.READ_ONLY),
                new ActionDescriptor(CLIENT_CREATE, ExecutionMode.MUTATING),
                new ActionDescriptor(CLIENT_UPDATE, ExecutionMode.MUTATING),
                new ActionDescriptor(CLIENT_DELETE, ExecutionMode.MUTATING),
                new ActionDescriptor(CLIENT_RESET_SECRET, ExecutionMode.MUTATING)),
                List.of(ScopeKind.ALL), List.of(), Map.of(), CLIENT_READ_LIST, false, false);
    }
}
