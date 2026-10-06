package com.ingot.framework.commons.model.iam.extension;

import java.util.*;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;

/**
 * <p>独立资源服务返回最小候选或批量存在性结果。</p>
 *
 * @param candidates 查询候选时返回
 * @param exists 存在性检查时返回
 * @author jy
 * @since 1.0.0
 */
public record ResourceObjectResult(AuthorizationCandidatePage candidates, Boolean exists) {

}
