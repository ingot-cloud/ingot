package com.ingot.cloud.pms.api.model.transform;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNode;
import com.ingot.framework.core.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : AuthorityTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/8/27.</p>
 * <p>Time         : 5:21 下午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface AuthorityTrans {

    AuthorityTreeNode to(SysAuthority params);
}
