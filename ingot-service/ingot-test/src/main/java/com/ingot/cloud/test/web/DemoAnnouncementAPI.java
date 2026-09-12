package com.ingot.cloud.test.web;

import java.util.List;

import com.ingot.cloud.test.authorization.DemoDataScopeConstants;
import com.ingot.cloud.test.model.domain.TDemoAnnouncement;
import com.ingot.cloud.test.service.domain.TDemoAnnouncementService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.data.mybatis.scope.config.DataScope;
import com.ingot.framework.data.mybatis.scope.guard.DataScopeGuard;
import com.ingot.framework.data.mybatis.scope.guard.DataScopeTarget;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>示例公告接口，范围与订单相互隔离，ALL 公告不得扩大订单可见集。</p>
 *
 * <p>功能准入与数据范围分注解，与订单示例相同。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/demo/announcements")
@RequiredArgsConstructor
public class DemoAnnouncementAPI implements RShortcuts {

    private final TDemoAnnouncementService announcementService;

    /**
     * 按当前用户公告查询范围列出记录。
     *
     * @return 可见公告
     */
    @AdminOrHasAnyAuthority({DemoDataScopeConstants.PERM_ANNOUNCEMENT_QUERY})
    @DataScope(resource = DemoDataScopeConstants.RESOURCE_ANNOUNCEMENT,
            permission = DemoDataScopeConstants.PERM_ANNOUNCEMENT_QUERY)
    @GetMapping
    public R<List<TDemoAnnouncement>> list() {
        return ok(announcementService.list());
    }

    /**
     * 创建公告并校验写入归属。
     *
     * @param announcement 公告
     * @return 主键
     */
    @AdminOrHasAnyAuthority({DemoDataScopeConstants.PERM_ANNOUNCEMENT_CREATE})
    @PostMapping
    public R<Long> create(@RequestBody TDemoAnnouncement announcement) {
        fillOwner(announcement);
        DataScopeGuard.assertWritable(DemoDataScopeConstants.RESOURCE_ANNOUNCEMENT,
                DemoDataScopeConstants.PERM_ANNOUNCEMENT_CREATE,
                DataScopeTarget.builder().deptId(announcement.getDeptId()).userId(announcement.getOwnerUserId()).build());
        announcementService.save(announcement);
        return ok(announcement.getId());
    }

    /**
     * 更新公告：先满足原记录范围，再校验新归属。
     *
     * @param id           主键
     * @param announcement 更新内容
     * @return 空成功
     */
    @AdminOrHasAnyAuthority({DemoDataScopeConstants.PERM_ANNOUNCEMENT_UPDATE})
    @DataScope(resource = DemoDataScopeConstants.RESOURCE_ANNOUNCEMENT,
            permission = DemoDataScopeConstants.PERM_ANNOUNCEMENT_UPDATE)
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody TDemoAnnouncement announcement) {
        TDemoAnnouncement current = announcementService.getById(id);
        if (current == null) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        if (announcement.getDeptId() != null) {
            current.setDeptId(announcement.getDeptId());
        }
        if (announcement.getOwnerUserId() != null) {
            current.setOwnerUserId(announcement.getOwnerUserId());
        }
        if (announcement.getTitle() != null) {
            current.setTitle(announcement.getTitle());
        }
        DataScopeGuard.assertWritable(DemoDataScopeConstants.RESOURCE_ANNOUNCEMENT,
                DemoDataScopeConstants.PERM_ANNOUNCEMENT_UPDATE,
                DataScopeTarget.builder().deptId(current.getDeptId()).userId(current.getOwnerUserId()).build());
        announcementService.updateById(current);
        return ok();
    }

    /**
     * 删除可见范围内的公告。
     *
     * @param id 主键
     * @return 空成功
     */
    @AdminOrHasAnyAuthority({DemoDataScopeConstants.PERM_ANNOUNCEMENT_DELETE})
    @DataScope(resource = DemoDataScopeConstants.RESOURCE_ANNOUNCEMENT,
            permission = DemoDataScopeConstants.PERM_ANNOUNCEMENT_DELETE)
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        TDemoAnnouncement current = announcementService.getById(id);
        if (current == null) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        announcementService.removeById(id);
        return ok();
    }

    private static void fillOwner(TDemoAnnouncement announcement) {
        InUser user = SecurityAuthContext.getUser();
        if (announcement.getOwnerUserId() == null && user != null) {
            announcement.setOwnerUserId(user.getId());
        }
    }
}
