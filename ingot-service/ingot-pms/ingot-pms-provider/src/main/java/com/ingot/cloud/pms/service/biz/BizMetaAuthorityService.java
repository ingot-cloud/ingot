package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;

/**
 * <p>Description  : BizMetaAuthorityService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 14:52.</p>
 */
public interface BizMetaAuthorityService {

    /**
     * 获取权限树
     *
     * @param filter {@link MetaAuthority} 过滤条件
     * @return {@link AuthorityTreeNodeVO}
     */
    List<AuthorityTreeNodeVO> treeList(MetaAuthority filter);

    /**
     * 创建非菜单权限
     *
     * @param authority {@link MetaAuthority}
     */
    void createNonMenuAuthority(MetaAuthority authority);

    /**
     * 修改非菜单权限
     *
     * @param authority {@link MetaAuthority}
     */
    void updateNonMenuAuthority(MetaAuthority authority);

    /**
     * 删除非菜单权限，会删除一些绑定数据
     *
     * @param id 权限ID
     */
    void deleteNonMenuAuthority(long id);
}
