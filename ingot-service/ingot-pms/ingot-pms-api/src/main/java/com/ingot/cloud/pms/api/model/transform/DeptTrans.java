package com.ingot.cloud.pms.api.model.transform;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.framework.core.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : DeptTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/29.</p>
 * <p>Time         : 9:47 下午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface DeptTrans {

    DeptTreeNodeVO to(SysDept params);
}
