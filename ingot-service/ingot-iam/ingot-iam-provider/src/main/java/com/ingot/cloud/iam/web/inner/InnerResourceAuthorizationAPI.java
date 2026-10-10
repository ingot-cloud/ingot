package com.ingot.cloud.iam.web.inner;

import com.ingot.framework.authorization.AuthorizationClient;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>新 IAM 内部求值只恢复认证身份，不接受替代账号或成员。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequiredArgsConstructor
@Permit(mode = PermitMode.INNER)
@RequestMapping("/inner/authorization/v2")
public class InnerResourceAuthorizationAPI implements RShortcuts {

    private final AuthorizationClient authorization;

    /**
     * 求值资源精确操作、对象范围与字段规则。
     * @param request 服务器已接入的资源及操作
     * @return 不含敏感原值的授权决策
     */
    @PostMapping("/evaluate")
    public R<AuthorizationDecision> evaluate(@Valid @RequestBody AuthorizationRequest request) {
        return ok(authorization.evaluate(request));
    }

    /** 预览当前认证身份的交互能力，不作为最终写放行依据。 */
    @PostMapping("/preview")
    public R<AuthorizationDecision> preview(@Valid @RequestBody AuthorizationRequest request) {
        return ok(authorization.preview(request));
    }
}
