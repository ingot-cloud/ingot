package com.ingot.cloud.pms.api.model.transform;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNode;
import com.ingot.framework.core.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : MenuTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/29.</p>
 * <p>Time         : 10:07 上午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface MenuTrans {

    MenuTreeNode to(SysMenu params);
}
