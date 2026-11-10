package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformMemberEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.infrastructure.persistence.IamPersistence;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformGroupMemberEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_platform_group_member，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamPlatformGroupMemberMapper extends BaseMapper<IamPlatformGroupMemberEntity> {
    /**
     * 查询当前页各组内符合成员读取范围的直接关联。
     *
     * @param groupIds 已分页的组 ID
     * @param scope 按成员表编译的读取范围
     * @return 组与可见直接成员关联
     */
    @Select("""
            <script>
            SELECT link.group_id, link.member_id FROM iam_platform_group_member link
            JOIN iam_platform_member ON iam_platform_member.id=link.member_id
              AND link.group_id IN
              <foreach collection="pageGroupIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ${ew.customSqlSegment}
            ORDER BY link.group_id, link.member_id
            </script>
            """)
    List<IamPlatformGroupMemberEntity> visibleGroupMembers(
            @Param("pageGroupIds") List<BigInteger> groupIds,
            @Param(Constants.WRAPPER) Wrapper<IamPlatformMemberEntity> scope);
}
