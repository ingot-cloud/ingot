package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.DepartmentLeaderSql;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.projection.DepartmentLeaderRow;
import org.apache.ibatis.annotations.*;

/**
 * <p>负责人关系的显式租户持久化；仅供已校验身份和对象范围的服务使用。</p>
 * @author jy
 * @since 1.0.0
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamDepartmentLeaderMapper {
    /** 查询已过滤的候选或关联分页。 */
    @SelectProvider(type = DepartmentLeaderSql.class, method = "page")
    List<DepartmentLeaderRow> page(@Param("q") DepartmentLeaderSql.Query query);

    /** 按同一查询范围统计。 */
    @SelectProvider(type = DepartmentLeaderSql.class, method = "count")
    long count(@Param("q") DepartmentLeaderSql.Query query);

    /** 批量统计关系和主负责人。 */
    @SelectProvider(type = DepartmentLeaderSql.class, method = "summaries")
    List<DepartmentLeaderRow> summaries(@Param("q") DepartmentLeaderSql.Query query);

    /** 部门持锁后读取完整关系，仅用于事务校验，不直接返回客户端。 */
    @Select("SELECT member_id,is_primary AS primary_flag FROM iam_department_leader WHERE tenant_id=#{tenant} AND department_id=#{department} ORDER BY member_id FOR UPDATE")
    List<DepartmentLeaderRow> lockRelations(@Param("tenant") BigInteger tenant, @Param("department") BigInteger department);

    /** 新增非主负责人，随后在同事务统一设主。 */
    @Insert("INSERT INTO iam_department_leader(tenant_id,department_id,member_id,is_primary) VALUES(#{tenant},#{department},#{member},FALSE)")
    int add(@Param("tenant") BigInteger tenant, @Param("department") BigInteger department, @Param("member") BigInteger member);

    /** 删除已通过范围校验的一条关系。 */
    @Delete("DELETE FROM iam_department_leader WHERE tenant_id=#{tenant} AND department_id=#{department} AND member_id=#{member}")
    int remove(@Param("tenant") BigInteger tenant, @Param("department") BigInteger department, @Param("member") BigInteger member);

    /** 变更主负责人前清除旧标记，避免唯一约束中间态冲突。 */
    @Update("UPDATE iam_department_leader SET is_primary=FALSE WHERE tenant_id=#{tenant} AND department_id=#{department} AND is_primary=TRUE")
    int clearPrimary(@Param("tenant") BigInteger tenant, @Param("department") BigInteger department);

    /** 在当前完整关系中指定唯一主负责人。 */
    @Update("UPDATE iam_department_leader SET is_primary=TRUE WHERE tenant_id=#{tenant} AND department_id=#{department} AND member_id=#{member}")
    int primary(@Param("tenant") BigInteger tenant, @Param("department") BigInteger department, @Param("member") BigInteger member);

    /** 用于删除及变更为根部门前的关联检查。 */
    @Select("SELECT COUNT(*) FROM iam_department_leader WHERE tenant_id=#{tenant} AND department_id=#{department}")
    long relationCount(@Param("tenant") BigInteger tenant, @Param("department") BigInteger department);

    /** 当前身份必须先持有租户组织锁，再锁成员与账号并检查其可用状态。 */
    @Select("SELECT m.id AS member_id,m.status,(m.status=#{activeStatus} AND a.enabled=TRUE AND a.deleted_at IS NULL) AS available FROM iam_tenant_member m JOIN iam_account a ON a.id=m.account_id WHERE m.tenant_id=#{tenant} AND m.id=#{member} FOR UPDATE")
    DepartmentLeaderRow lockMember(@Param("tenant") BigInteger tenant, @Param("member") BigInteger member,
                                   @Param("activeStatus") String activeStatus);

    /** 成功变更关系后递增同一个部门版本。 */
    @Update("UPDATE iam_department SET version=version+1 WHERE tenant_id=#{tenant} AND id=#{department} AND version=#{version}")
    int incrementVersion(@Param("tenant") BigInteger tenant, @Param("department") BigInteger department,
                         @Param("version") BigInteger version);

    /** 读取该成员当前负责的部门，供重新加入前清理。 */
    @Select("SELECT department_id FROM iam_department_leader WHERE tenant_id=#{tenant} AND member_id=#{member} ORDER BY department_id")
    List<BigInteger> departmentsOfMember(@Param("tenant") BigInteger tenant, @Param("member") BigInteger member);

    /** 删除该成员在当前租户的全部负责人关系。 */
    @Delete("DELETE FROM iam_department_leader WHERE tenant_id=#{tenant} AND member_id=#{member}")
    int removeMember(@Param("tenant") BigInteger tenant, @Param("member") BigInteger member);

    /** 清理负责人后递增部门版本，不匹配读取时的旧版本。 */
    @Update("UPDATE iam_department SET version=version+1 WHERE tenant_id=#{tenant} AND id=#{department}")
    int bumpVersion(@Param("tenant") BigInteger tenant, @Param("department") BigInteger department);
}
