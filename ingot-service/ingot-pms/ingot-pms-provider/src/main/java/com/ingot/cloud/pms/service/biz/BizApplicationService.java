package com.ingot.cloud.pms.service.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysApplication;
import com.ingot.cloud.pms.api.model.dto.application.ApplicationFilterDTO;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationPageItemVO;
import com.ingot.framework.core.model.common.RelationDTO;

import java.util.List;

/**
 * <p>Description  : 应用服务，应用=权限+菜单</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/22.</p>
 * <p>Time         : 17:41.</p>
 */
public interface BizApplicationService {

    /**
     * 应用分页
     *
     * @param page   分页参数
     * @param filter 过滤参数
     * @return {@link ApplicationPageItemVO}
     */
    IPage<ApplicationPageItemVO> page(Page<SysApplication> page, ApplicationFilterDTO filter);

    /**
     * 获取组织当前开通的应用信息
     *
     * @param orgId 组织ID
     * @return {@link ApplicationPageItemVO}
     */
    List<ApplicationPageItemVO> orgApplicationList(long orgId);

    /**
     * 创建应用，如果创建的时候默认应用，那么同步所有组织
     *
     * @param params {@link SysApplication}
     */
    void createApplication(SysApplication params);

    /**
     * 更新应用, 可更新应用状态，更新后可能影响所有组织
     *
     * @param params {@link SysApplication}
     */
    void updateApplicationStatus(SysApplication params);

    /**
     * 更新应用默认状态，更新后会影响所有组织
     *
     * @param params {@link SysApplication}
     */
    void updateApplicationDefault(SysApplication params);

    /**
     * 移除应用，如果移除的是默认应用，那么会影响所有组织
     *
     * @param id 应用ID
     */
    void removeApplication(long id);

    /**
     * 组织绑定应用
     *
     * @param params {@link RelationDTO}
     */
    void bindApplication(RelationDTO<Long, Long> params);

}
