package com.fco.platform.player.interfaces.mapper;

import com.fco.platform.card.domain.FcoCardPrice;
import com.fco.platform.card.domain.FcoSeason;
import com.fco.platform.card.domain.FcoTrait;
import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.player.domain.FcoClub;
import com.fco.platform.player.domain.FcoPlayer;
import com.fco.platform.player.interfaces.dto.PlayerDetailResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-19T18:25:56+0700",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 23.0.2 (Eclipse Adoptium)"
)
@Component
public class PlayerDetailMapperImpl implements PlayerDetailMapper {

    @Override
    public PlayerDetailResponse toDetailResponse(PlayerCard card) {
        if ( card == null ) {
            return null;
        }

        PlayerDetailResponse.PlayerDetailResponseBuilder playerDetailResponse = PlayerDetailResponse.builder();

        playerDetailResponse.id( card.getId() );
        playerDetailResponse.externalId( cardPlayerExternalId( card ) );
        playerDetailResponse.playerName( cardPlayerPlayerName( card ) );
        playerDetailResponse.seasonCode( cardSeasonSeasonCode( card ) );
        playerDetailResponse.height( cardPlayerHeight( card ) );
        playerDetailResponse.weight( cardPlayerWeight( card ) );
        playerDetailResponse.birthdate( cardPlayerBirthdate( card ) );
        playerDetailResponse.preferredFoot( cardPlayerPreferredFoot( card ) );
        playerDetailResponse.weakFoot( cardPlayerWeakFoot( card ) );
        playerDetailResponse.nationName( cardPlayerNationName( card ) );
        playerDetailResponse.leagueName( cardPlayerLeagueName( card ) );
        Set<FcoClub> teamColors = cardPlayerTeamColors( card );
        playerDetailResponse.clubs( fcoClubSetToClubResponseList( teamColors ) );
        playerDetailResponse.traits( fcoTraitSetToTraitResponseList( card.getTraits() ) );
        playerDetailResponse.prices( fcoCardPriceListToPriceResponseList( card.getPrices() ) );
        playerDetailResponse.enhanceLevel( card.getEnhanceLevel() );
        playerDetailResponse.ovr( card.getOvr() );
        playerDetailResponse.salary( card.getSalary() );
        playerDetailResponse.preferredPosition( card.getPreferredPosition() );
        playerDetailResponse.marketPriceBp( card.getMarketPriceBp() );
        playerDetailResponse.imageUrl( card.getImageUrl() );
        playerDetailResponse.pace( card.getPace() );
        playerDetailResponse.shooting( card.getShooting() );
        playerDetailResponse.passing( card.getPassing() );
        playerDetailResponse.dribbling( card.getDribbling() );
        playerDetailResponse.defending( card.getDefending() );
        playerDetailResponse.physicality( card.getPhysicality() );
        playerDetailResponse.liveperf( card.getLiveperf() );
        playerDetailResponse.skillLevel( card.getSkillLevel() );
        playerDetailResponse.secondaryPosition( card.getSecondaryPosition() );
        playerDetailResponse.workerateAtt( card.getWorkerateAtt() );
        playerDetailResponse.workerateDef( card.getWorkerateDef() );
        playerDetailResponse.bodytype( card.getBodytype() );
        playerDetailResponse.reputation( card.getReputation() );
        playerDetailResponse.priceUpdatedAt( card.getPriceUpdatedAt() );
        Map<String, Integer> map = card.getOvrByPos();
        if ( map != null ) {
            playerDetailResponse.ovrByPos( new LinkedHashMap<String, Integer>( map ) );
        }

        playerDetailResponse.nationSlug( card.getPlayer().getNation() != null ? card.getPlayer().getNation().getNationSlug() : null );
        playerDetailResponse.leagueSlug( card.getPlayer().getLeague() != null ? card.getPlayer().getLeague().getLeagueSlug() : null );
        playerDetailResponse.hasLivePerf( card.getLiveperf() != null && card.getLiveperf() > 0 );

        return playerDetailResponse.build();
    }

    @Override
    public PlayerDetailResponse.ClubResponse mapClub(FcoClub club) {
        if ( club == null ) {
            return null;
        }

        PlayerDetailResponse.ClubResponse.ClubResponseBuilder clubResponse = PlayerDetailResponse.ClubResponse.builder();

        clubResponse.clubName( club.getClubName() );
        clubResponse.clubSlug( club.getClubSlug() );
        clubResponse.clubFifaaddictId( club.getFifaaddictId() );
        clubResponse.crestUrl( club.getCrestUrl() );

        return clubResponse.build();
    }

    @Override
    public PlayerDetailResponse.TraitResponse mapTrait(FcoTrait trait) {
        if ( trait == null ) {
            return null;
        }

        PlayerDetailResponse.TraitResponse.TraitResponseBuilder traitResponse = PlayerDetailResponse.TraitResponse.builder();

        traitResponse.traitCode( trait.getTraitCode() );
        traitResponse.traitName( trait.getTraitName() );
        traitResponse.description( trait.getDescription() );
        traitResponse.iconId( trait.getIconId() );
        traitResponse.iconUrl( trait.getIconUrl() );

        return traitResponse.build();
    }

    @Override
    public PlayerDetailResponse.PriceResponse mapPrice(FcoCardPrice price) {
        if ( price == null ) {
            return null;
        }

        PlayerDetailResponse.PriceResponse.PriceResponseBuilder priceResponse = PlayerDetailResponse.PriceResponse.builder();

        priceResponse.grade( price.getGrade() );
        priceResponse.priceBp( price.getPriceBp() );
        priceResponse.priceRaw( price.getPriceRaw() );
        priceResponse.priceDate( price.getPriceDate() );

        return priceResponse.build();
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

    private Integer cardPlayerHeight(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        Integer height = player.getHeight();
        if ( height == null ) {
            return null;
        }
        return height;
    }

    private Integer cardPlayerWeight(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        Integer weight = player.getWeight();
        if ( weight == null ) {
            return null;
        }
        return weight;
    }

    private String cardPlayerBirthdate(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        String birthdate = player.getBirthdate();
        if ( birthdate == null ) {
            return null;
        }
        return birthdate;
    }

    private String cardPlayerPreferredFoot(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        String preferredFoot = player.getPreferredFoot();
        if ( preferredFoot == null ) {
            return null;
        }
        return preferredFoot;
    }

    private Integer cardPlayerWeakFoot(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        Integer weakFoot = player.getWeakFoot();
        if ( weakFoot == null ) {
            return null;
        }
        return weakFoot;
    }

    private String cardPlayerNationName(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        String nationName = player.getNationName();
        if ( nationName == null ) {
            return null;
        }
        return nationName;
    }

    private String cardPlayerLeagueName(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        String leagueName = player.getLeagueName();
        if ( leagueName == null ) {
            return null;
        }
        return leagueName;
    }

    private Set<FcoClub> cardPlayerTeamColors(PlayerCard playerCard) {
        if ( playerCard == null ) {
            return null;
        }
        FcoPlayer player = playerCard.getPlayer();
        if ( player == null ) {
            return null;
        }
        Set<FcoClub> teamColors = player.getTeamColors();
        if ( teamColors == null ) {
            return null;
        }
        return teamColors;
    }

    protected List<PlayerDetailResponse.ClubResponse> fcoClubSetToClubResponseList(Set<FcoClub> set) {
        if ( set == null ) {
            return null;
        }

        List<PlayerDetailResponse.ClubResponse> list = new ArrayList<PlayerDetailResponse.ClubResponse>( set.size() );
        for ( FcoClub fcoClub : set ) {
            list.add( mapClub( fcoClub ) );
        }

        return list;
    }

    protected List<PlayerDetailResponse.TraitResponse> fcoTraitSetToTraitResponseList(Set<FcoTrait> set) {
        if ( set == null ) {
            return null;
        }

        List<PlayerDetailResponse.TraitResponse> list = new ArrayList<PlayerDetailResponse.TraitResponse>( set.size() );
        for ( FcoTrait fcoTrait : set ) {
            list.add( mapTrait( fcoTrait ) );
        }

        return list;
    }

    protected List<PlayerDetailResponse.PriceResponse> fcoCardPriceListToPriceResponseList(List<FcoCardPrice> list) {
        if ( list == null ) {
            return null;
        }

        List<PlayerDetailResponse.PriceResponse> list1 = new ArrayList<PlayerDetailResponse.PriceResponse>( list.size() );
        for ( FcoCardPrice fcoCardPrice : list ) {
            list1.add( mapPrice( fcoCardPrice ) );
        }

        return list1;
    }
}
