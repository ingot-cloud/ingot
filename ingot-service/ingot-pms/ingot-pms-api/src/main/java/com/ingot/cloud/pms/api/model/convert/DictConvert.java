package com.ingot.cloud.pms.api.model.convert;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformDict;
import com.ingot.cloud.pms.api.model.vo.dict.DictItemVO;
import com.ingot.cloud.pms.api.model.vo.dict.DictTreeNodeVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * 字典对象转换器。
 *
 * @author jy
 * @since 2026/4/25
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface DictConvert {

    DictItemVO toItem(PlatformDict source);

    List<DictItemVO> toItems(List<PlatformDict> source);

    DictTreeNodeVO toTreeNode(PlatformDict source);

    List<DictTreeNodeVO> toTreeNodes(List<PlatformDict> source);
}
