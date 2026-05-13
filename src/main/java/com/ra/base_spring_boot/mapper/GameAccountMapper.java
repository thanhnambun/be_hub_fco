package com.ra.base_spring_boot.mapper;

import com.ra.base_spring_boot.dto.resp.GameAccountResponse;
import com.ra.base_spring_boot.model.GameAccount;
import org.mapstruct.Mapper;

@Mapper(config = MapStructCentralConfig.class)
public interface GameAccountMapper {
    GameAccountResponse toResponse(GameAccount gameAccount);
}
