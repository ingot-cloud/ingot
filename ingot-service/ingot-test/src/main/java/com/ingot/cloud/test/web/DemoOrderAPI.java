package com.ingot.cloud.test.web;

import java.util.List;

import com.ingot.cloud.test.authorization.DemoDataScopeConstants;
import com.ingot.cloud.test.model.domain.TDemoOrder;
import com.ingot.cloud.test.service.domain.TDemoOrderService;
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
 * <p>示例订单接口，演示 A 部门主管与 B 部门成员的范围隔离。</p>
 *
 * <p>功能准入用 {@code @AdminOrHasAnyAuthority}，行过滤用 {@code @DataScope}，创建与改归属用 {@code DataScopeGuard}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/demo/orders")
@RequiredArgsConstructor
public class DemoOrderAPI implements RShortcuts {

    private final TDemoOrderService orderService;

    /**
     * 按当前用户订单查询范围列出记录。
     *
     * @return 可见订单
     */
    @AdminOrHasAnyAuthority({DemoDataScopeConstants.PERM_ORDER_QUERY})
    @DataScope(resource = DemoDataScopeConstants.RESOURCE_ORDER, permission = DemoDataScopeConstants.PERM_ORDER_QUERY)
    @GetMapping
    public R<List<TDemoOrder>> list() {
        return ok(orderService.list());
    }

    /**
     * 创建订单并校验写入归属。
     *
     * @param order 订单
     * @return 主键
     */
    @AdminOrHasAnyAuthority({DemoDataScopeConstants.PERM_ORDER_CREATE})
    @PostMapping
    public R<Long> create(@RequestBody TDemoOrder order) {
        fillOwner(order);
        DataScopeGuard.assertWritable(DemoDataScopeConstants.RESOURCE_ORDER,
                DemoDataScopeConstants.PERM_ORDER_CREATE,
                DataScopeTarget.builder().deptId(order.getDeptId()).userId(order.getOwnerUserId()).build());
        orderService.save(order);
        return ok(order.getId());
    }

    /**
     * 更新订单：先满足原记录范围，再校验新归属。
     *
     * @param id    主键
     * @param order 更新内容
     * @return 空成功
     */
    @AdminOrHasAnyAuthority({DemoDataScopeConstants.PERM_ORDER_UPDATE})
    @DataScope(resource = DemoDataScopeConstants.RESOURCE_ORDER, permission = DemoDataScopeConstants.PERM_ORDER_UPDATE)
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody TDemoOrder order) {
        TDemoOrder current = orderService.getById(id);
        if (current == null) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        if (order.getDeptId() != null) {
            current.setDeptId(order.getDeptId());
        }
        if (order.getOwnerUserId() != null) {
            current.setOwnerUserId(order.getOwnerUserId());
        }
        if (order.getTitle() != null) {
            current.setTitle(order.getTitle());
        }
        DataScopeGuard.assertWritable(DemoDataScopeConstants.RESOURCE_ORDER,
                DemoDataScopeConstants.PERM_ORDER_UPDATE,
                DataScopeTarget.builder().deptId(current.getDeptId()).userId(current.getOwnerUserId()).build());
        orderService.updateById(current);
        return ok();
    }

    /**
     * 删除可见范围内的订单。
     *
     * @param id 主键
     * @return 空成功
     */
    @AdminOrHasAnyAuthority({DemoDataScopeConstants.PERM_ORDER_DELETE})
    @DataScope(resource = DemoDataScopeConstants.RESOURCE_ORDER, permission = DemoDataScopeConstants.PERM_ORDER_DELETE)
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        TDemoOrder current = orderService.getById(id);
        if (current == null) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        orderService.removeById(id);
        return ok();
    }

    private static void fillOwner(TDemoOrder order) {
        InUser user = SecurityAuthContext.getUser();
        if (order.getOwnerUserId() == null && user != null) {
            order.setOwnerUserId(user.getId());
        }
    }
}
