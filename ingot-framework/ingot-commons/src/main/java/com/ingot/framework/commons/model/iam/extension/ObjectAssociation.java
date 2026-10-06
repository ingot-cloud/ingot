package com.ingot.framework.commons.model.iam.extension;

import java.util.*;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.*;

/**
 * <p>服务端确认的已选关联，不接受客户端伪造授权来源。</p>
 *
 * @param kind 关联为分配或委派
 * @param id 关联记录ID
 * @param parameterKey 分配参数键
 * @param actionId 委派上限操作ID
 * @author jy
 * @since 1.0.0
 */
public record ObjectAssociation(ObjectAssociationKind kind, String id, String parameterKey, String actionId) {

}
