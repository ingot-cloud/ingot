package com.ingot.cloud.auth.model.convert;

import com.ingot.cloud.auth.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.auth.model.dto.OAuth2RegisteredClientDTO;
import com.ingot.cloud.auth.model.vo.OAuth2RegisteredClientVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : ClientTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/13.</p>
 * <p>Time         : 9:55 上午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface ClientConvert {

    Oauth2RegisteredClient to(OAuth2RegisteredClientDTO in);

    OAuth2RegisteredClientVO to(Oauth2RegisteredClient in);
}
