package com.fco.platform.card.interfaces.mapper;

import com.fco.platform.card.domain.FcoSeason;
import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.player.domain.FcoPlayer;
import com.fco.platform.player.interfaces.dto.PlayerCardResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-19T18:25:55+0700",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 23.0.2 (Eclipse Adoptium)"
)
@Component
public class PlayerCardMapperImpl implements PlayerCardMapper {

    @Override
    public PlayerCardResponse toResponse(PlayerCard card) {
        if ( card == null ) {
            return null;
        }

        PlayerCardResponse playerCardResponse = new PlayerCardResponse();

        playerCardResponse.setExternalId( cardPlayerExternalId( card ) );
        playerCardResponse.setPlayerName( cardPlayerPlayerName( card ) );
        playerCardResponse.setSeasonCode( cardSeasonSeasonCode( card ) );
        playerCardResponse.setPreferredPosition( card.getPreferredPosition() );
        playerCardResponse.setId( card.getId() );
        playerCardResponse.setOvr( card.getOvr() );
        playerCardResponse.setSalary( card.getSalary() );
        playerCardResponse.setMarketPriceBp( card.getMarketPriceBp() );
        playerCardResponse.setImageUrl( card.getImageUrl() );
        playerCardResponse.setPace( card.getPace() );
        playerCardResponse.setShooting( card.getShooting() );
        playerCardResponse.setPassing( card.getPassing() );
        playerCardResponse.setDribbling( card.getDribbling() );
        playerCardResponse.setDefending( card.getDefending() );
        playerCardResponse.setPhysicality( card.getPhysicality() );

        return playerCardResponse;
    }

    private String cardPlayerExternalId(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        String externalId = player.getExternalId();
        if ( externalId == null ) {
            return null;
        }
        return externalId;
    }

    private String cardPlayerPlayerName(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        String playerName = player.getPlayerName();
        if ( playerName == null ) {
            return null;
        }
        return playerName;
    }

    private String cardSeasonSeasonCode(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoSeason season = playerCard.getSeason();
        if ( season == null ) {
            return null;
        }
        String seasonCode = season.getSeasonCode();
        if ( seasonCode == null ) {
            return null;
        }
        return seasonCode;
    }
}
