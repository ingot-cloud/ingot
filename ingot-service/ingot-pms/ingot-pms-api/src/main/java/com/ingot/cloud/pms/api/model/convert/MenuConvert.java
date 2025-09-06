package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : MenuTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/29.</p>
 * <p>Time         : 10:07 上午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface MenuConvert {

    MenuTreeNodeVO to(SysMenu params);

    SysMenu to(MenuTreeNodeVO in);

    SysMenu copy(SysMenu in);
}
