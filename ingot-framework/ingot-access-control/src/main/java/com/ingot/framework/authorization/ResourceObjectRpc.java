package com.ingot.framework.authorization;

import com.ingot.framework.commons.model.iam.extension.ResourceObjectResult;
import com.ingot.framework.commons.model.iam.extension.SignedResourceObjectRequest;
import com.ingot.framework.commons.model.support.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>固定内部对象查询契约，服务名由可信配置提供。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface ResourceObjectRpc {

    /**
     * 内部固定路径。
     */
    String PATH = "/inner/iam/resource-objects/query";

    /**
     * 请求分页候选或批量存在性。
     * @param request 签名封装
     * @return 最小信息
     */
    @PostMapping(PATH)
    R<ResourceObjectResult> query(@RequestBody SignedResourceObjectRequest request);

}
