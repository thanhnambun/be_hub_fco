package com.fco.platform.player.interfaces.mapper;

import com.fco.platform.card.domain.FcoCardPrice;
import com.fco.platform.card.domain.FcoTrait;
import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.common.mapper.MapStructCentralConfig;
import com.fco.platform.player.domain.FcoClub;
import com.fco.platform.player.interfaces.dto.PlayerDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(config = MapStructCentralConfig.class)
public interface PlayerDetailMapper {

    @Mapping(target = "id", source = "card.id")
    @Mapping(target = "externalId", source = "card.player.externalId")
    @Mapping(target = "playerName", source = "card.player.playerName")
    @Mapping(target = "seasonCode", source = "card.season.seasonCode")
    @Mapping(target = "height", source = "card.player.height")
    @Mapping(target = "weight", source = "card.player.weight")
    @Mapping(target = "birthdate", source = "card.player.birthdate")
    @Mapping(target = "preferredFoot", source = "card.player.preferredFoot")
    @Mapping(target = "weakFoot", source = "card.player.weakFoot")
    @Mapping(target = "nationName", source = "card.player.nationName")
    @Mapping(target = "nationSlug", expression = "java(card.getPlayer().getNation() != null ? card.getPlayer().getNation().getNationSlug() : null)")
    @Mapping(target = "leagueName", source = "card.player.leagueName")
    @Mapping(target = "leagueSlug", expression = "java(card.getPlayer().getLeague() != null ? card.getPlayer().getLeague().getLeagueSlug() : null)")
    @Mapping(target = "hasLivePerf", expression = "java(card.getLiveperf() != null && card.getLiveperf() > 0)")
    @Mapping(target = "clubs", source = "card.player.teamColors")
    @Mapping(target = "traits", source = "card.traits")
    @Mapping(target = "prices", source = "card.prices")
    PlayerDetailResponse toDetailResponse(PlayerCard card);

    @Mapping(target = "clubName", source = "clubName")
    @Mapping(target = "clubSlug", source = "clubSlug")
    @Mapping(target = "clubFifaaddictId", source = "fifaaddictId")
    @Mapping(target = "crestUrl", source = "crestUrl")
    PlayerDetailResponse.ClubResponse mapClub(FcoClub club);

    @Mapping(target = "traitCode", source = "traitCode")
    @Mapping(target = "traitName", source = "traitName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "iconId", source = "iconId")
    @Mapping(target = "iconUrl", source = "iconUrl")
    PlayerDetailResponse.TraitResponse mapTrait(FcoTrait trait);

    @Mapping(target = "grade", source = "grade")
    @Mapping(target = "priceBp", source = "priceBp")
    @Mapping(target = "priceRaw", source = "priceRaw")
    @Mapping(target = "priceDate", source = "priceDate")
    PlayerDetailResponse.PriceResponse mapPrice(FcoCardPrice price);
}
