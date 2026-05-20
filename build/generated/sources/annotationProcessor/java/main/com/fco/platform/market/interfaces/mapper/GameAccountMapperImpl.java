package com.fco.platform.market.interfaces.mapper;

import com.fco.platform.market.domain.GameAccount;
import com.fco.platform.market.interfaces.dto.GameAccountResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-18T12:51:46+0700",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 23.0.2 (Eclipse Adoptium)"
)
@Component
public class GameAccountMapperImpl implements GameAccountMapper {

    @Override
    public GameAccountResponse toResponse(GameAccount gameAccount) {
        if ( gameAccount == null ) {
            return null;
        }

        GameAccountResponse gameAccountResponse = new GameAccountResponse();

        gameAccountResponse.setId( gameAccount.getId() );
        gameAccountResponse.setAccountCode( gameAccount.getAccountCode() );
        gameAccountResponse.setTitle( gameAccount.getTitle() );
        gameAccountResponse.setRankName( gameAccount.getRankName() );
        gameAccountResponse.setPrice( gameAccount.getPrice() );
        gameAccountResponse.setAccountStatus( gameAccount.getAccountStatus() );
        gameAccountResponse.setIsFeatured( gameAccount.getIsFeatured() );

        return gameAccountResponse;
    }
}
