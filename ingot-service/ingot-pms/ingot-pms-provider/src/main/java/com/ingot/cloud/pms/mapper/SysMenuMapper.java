package com.ingot.cloud.pms.mapper;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 获取所有菜单，菜单中返回关联的权限编码（如果权限ID不为空）
     * @return {@link MenuTreeNodeVO} list
     */
    List<MenuTreeNodeVO> getAll();
}
