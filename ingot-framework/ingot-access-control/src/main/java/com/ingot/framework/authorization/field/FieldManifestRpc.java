package com.ingot.framework.authorization.field;

import com.ingot.framework.commons.model.iam.extension.*;
import com.ingot.framework.commons.model.support.R;
import org.springframework.web.bind.annotation.*;

/**
 * <p>服务发现白名单调用的固定清单端点，不接受浏览器 URL 或业务值。</p>
 * @author jy
 * @since 1.0.0
 */
public interface FieldManifestRpc {
    /** 所有接入服务使用的内部固定路径。 */
    String PATH = "/inner/iam/field-bindings/manifest";
    /** 获取服务器签名请求指定的资源清单。 */
    @PostMapping(PATH)
    R<FieldBindingManifest> manifest(@RequestBody SignedResourceObjectRequest request);
}
