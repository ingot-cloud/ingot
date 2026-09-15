package com.ingot.cloud.iam.api.model.types;

import com.ingot.cloud.iam.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.iam.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;

/**
 * <p>权限树节点共用字段：编码、节点类型、组织维度与状态。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface PermissionType {
    /**
     * ID
     */
    Long getId();

    void setId(Long id);

    /**
     * PID
     */
    Long getPid();

    void setPid(Long pid);

    Long getAppId();

    void setAppId(Long appId);

    /**
     * 权限名称
     */
    String getName();

    void setName(String name);

    /**
     * 权限编码
     */
    String getCode();

    void setCode(String code);

    /**
     * 节点类型，仅 GROUP 或 ACTION
     */
    PermissionNodeTypeEnum getNodeType();

    void setNodeType(PermissionNodeTypeEnum nodeType);

    /**
     * 关联资源 ID，非数据操作可空
     */
    Long getResourceId();

    void setResourceId(Long resourceId);

    /**
     * 组织类型
     */
    OrgTypeEnum getOrgType();

    void setOrgType(OrgTypeEnum type);

    /**
     * 状态, 0:正常，9:禁用
     */
    CommonStatusEnum getStatus();

    void setStatus(CommonStatusEnum status);

    /**
     * 备注
     */
    String getRemark();

    void setRemark(String remark);
}
