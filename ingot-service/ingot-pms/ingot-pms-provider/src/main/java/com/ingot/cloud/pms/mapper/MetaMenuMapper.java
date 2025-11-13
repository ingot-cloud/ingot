package com.ingot.cloud.pms.mapper;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Mapper
public interface MetaMenuMapper extends BaseMapper<MetaMenu> {

    /**
     * 获取所有菜单，菜单中返回关联的权限编码（如果权限ID不为空）
     *
     * @return {@link MenuTreeNodeVO} list
     */
    List<MenuTreeNodeVO> getAll();
}
