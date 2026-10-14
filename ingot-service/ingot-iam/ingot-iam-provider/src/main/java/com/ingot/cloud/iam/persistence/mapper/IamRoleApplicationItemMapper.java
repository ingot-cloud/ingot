package com.ingot.cloud.iam.persistence.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamRoleApplicationItemEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import com.ingot.cloud.iam.persistence.projection.RoleApplicationCount;

/**
 * <p>持久化版本应用数据，由Repository显式限制任务和对象边界。</p>
 * @author jy
 * @since 1.0.0
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamRoleApplicationItemMapper extends BaseMapper<IamRoleApplicationItemEntity> {
    /**
     * 按任务与状态聚合，分配范围必须与明细查询一致。
     * @param taskIds 已验证角色的当前页任务，非空
     * @param assignmentIds 可见分配；null 表示全部，非空集合表示受限范围
     * @return 有明细的状态聚合行
     */
    @Select("""
            <script>
            SELECT task_id AS taskId, status, COUNT(*) AS total
            FROM iam_role_application_item
            WHERE task_id IN
            <foreach collection="taskIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            <if test="assignmentIds != null">
              AND assignment_id IN
              <foreach collection="assignmentIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            GROUP BY task_id, status
            </script>
            """)
    List<RoleApplicationCount> countByTaskAndStatus(@Param("taskIds") Collection<BigInteger> taskIds,
            @Param("assignmentIds") Collection<BigInteger> assignmentIds);
}
