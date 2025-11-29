package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.vo.permission.BizPermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.permission.BizPermissionVO;
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

    PermissionTreeNodeVO toTreeNode(PermissionType params);

    BizPermissionTreeNodeVO to(BizPermissionVO params);

    BizPermissionVO to(MetaPermission in);
}
