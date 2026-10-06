package com.ingot.example.ops;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>运维工单示例，使用UUID对象标识；归属从持久化实体恢复。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName("ops_incident")
public class Incident {

    /**
     * 稳定UUID，不是IAM发号ID。
     */
    @TableId
    private String id;

    /**
     * 归属成员ID，不是账号ID。
     */
    private String ownerMemberId;

    /**
     * 标题。
     */
    private String title;

    /**
     * 敏感联系方式，由字段策略输出。
     */
    private String contact;

}
