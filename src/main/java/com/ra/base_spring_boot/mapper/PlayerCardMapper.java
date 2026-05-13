package com.ra.base_spring_boot.mapper;

import com.ra.base_spring_boot.dto.resp.PlayerCardResponse;
import com.ra.base_spring_boot.model.fco.PlayerCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructCentralConfig.class)
public interface PlayerCardMapper {
    @Mapping(target = "externalId", source = "player.externalId")
    @Mapping(target = "playerName", source = "player.playerName")
    @Mapping(target = "seasonCode", source = "season.seasonCode")
    @Mapping(target = "preferredPosition", source = "preferredPosition")
    PlayerCardResponse toResponse(PlayerCard card);
}
