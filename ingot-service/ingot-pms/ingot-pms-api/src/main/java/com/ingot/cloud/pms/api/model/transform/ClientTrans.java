package com.ingot.cloud.pms.api.model.transform;

import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.dto.client.OAuth2RegisteredClientDto;
import com.ingot.cloud.pms.api.model.vo.client.OAuth2RegisteredClientVo;
import com.ingot.framework.core.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : ClientTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/13.</p>
 * <p>Time         : 9:55 上午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface ClientTrans {

    Oauth2RegisteredClient to(OAuth2RegisteredClientDto in);

    OAuth2RegisteredClientVo to(Oauth2RegisteredClient in);
}
