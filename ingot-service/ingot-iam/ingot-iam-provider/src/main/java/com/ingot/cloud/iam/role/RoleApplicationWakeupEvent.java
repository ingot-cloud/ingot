package com.ingot.cloud.iam.role;

import java.math.BigInteger;

import com.ingot.framework.eventbus.EventType;
import com.ingot.framework.eventbus.InvalidationEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>跨节点角色应用领取提示，接收方必须以数据库事实重新校验。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@EventType(RoleApplicationWakeupEvent.TYPE)
public class RoleApplicationWakeupEvent extends InvalidationEvent {
    /** 框架事件总线通道类型，提示不作为可靠工作队列。 */
    public static final String TYPE = "iam.role-application.wakeup";
    /** 任务ID，按公开标识契约传字符串，不含身份信息。 */
    private String taskId;
    /** 关联角色ID，仅用于提示，不替代数据库关联校验。 */
    private String roleId;

    /** 从已提交的任务生成跨节点提示。 */
    public static RoleApplicationWakeupEvent of(BigInteger taskId, BigInteger roleId) {
        var event = new RoleApplicationWakeupEvent();
        event.setTaskId(taskId.toString());
        event.setRoleId(roleId.toString());
        return event;
    }
}
