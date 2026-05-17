package com.fco.platform.market.interfaces.mapper;

import com.fco.platform.common.mapper.MapStructCentralConfig;
import com.fco.platform.market.domain.GameAccount;
import com.fco.platform.market.interfaces.dto.GameAccountResponse;
import org.mapstruct.Mapper;

@Mapper(config = MapStructCentralConfig.class)
public interface GameAccountMapper {
    GameAccountResponse toResponse(GameAccount gameAccount);
}
