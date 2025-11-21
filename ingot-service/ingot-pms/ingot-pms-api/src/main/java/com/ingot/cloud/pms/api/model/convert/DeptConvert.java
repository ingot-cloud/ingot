package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptWithManagerVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : DeptTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/29.</p>
 * <p>Time         : 9:47 下午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface DeptConvert {

    DeptTreeNodeVO to(TenantDept params);

    DeptTreeNodeVO to(DeptWithManagerVO params);

    TenantDept to(DeptTreeNodeVO in);
}
