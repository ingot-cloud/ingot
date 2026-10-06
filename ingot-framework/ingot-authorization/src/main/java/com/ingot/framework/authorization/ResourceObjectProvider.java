package com.ingot.framework.authorization;

import java.util.List;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;

/**
 * <p>可信资源执行适配器，查询必须在业务数据库完成筛选和分页。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface ResourceObjectProvider {

    /**
     * 返回服务器注册描述。
     * @return 描述
     */
    ResourceDescriptor descriptor();

    /**
     * 执行已经限定身份及用途的候选查询。
     * @param query 可信查询
     * @return 候选页
     */
    AuthorizationCandidatePage candidates(ResourceObjectQuery query);

    /**
     * 是否可在业务数据库内原生关联IAM绑定表。
     * @return 默认false，独立服务由IAM编译成对象边界
     */
    default boolean nativeAssociations() {
        return false;
    }

    /**
     * 验证实际对象存在及域归属，不扩展访问权限。
     * @param context 身份
     * @param ids 实际标识
     * @return 全部有效
     */
    boolean objectsExist(AuthorizationContext context, List<String> ids);

}
