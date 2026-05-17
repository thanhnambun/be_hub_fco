package com.fco.platform.card.interfaces.mapper;

import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.player.interfaces.dto.PlayerCardResponse;
import com.fco.platform.common.mapper.MapStructCentralConfig;
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
