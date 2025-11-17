package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : AuthorityTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/8/27.</p>
 * <p>Time         : 5:21 下午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface AuthorityConvert {

    AuthorityTreeNodeVO to(AuthorityType params);
}
