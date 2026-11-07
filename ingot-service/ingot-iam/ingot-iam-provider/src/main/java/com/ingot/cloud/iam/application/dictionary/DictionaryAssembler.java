package com.ingot.cloud.iam.application.dictionary;

import java.util.List;

import com.ingot.cloud.iam.infrastructure.persistence.entity.DictionaryEntity;
import com.ingot.cloud.iam.api.model.vo.dict.DictItemVO;
import com.ingot.cloud.iam.application.dictionary.model.DictTreeNodeVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * 字典对象转换器。
 *
 * @author jy
 * @since 2026/4/25
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface DictionaryAssembler {

    DictItemVO toItem(DictionaryEntity source);

    List<DictItemVO> toItems(List<DictionaryEntity> source);

    DictTreeNodeVO toTreeNode(DictionaryEntity source);

    List<DictTreeNodeVO> toTreeNodes(List<DictionaryEntity> source);
}
