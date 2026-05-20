package com.fco.platform.player.application;

import com.fco.platform.player.interfaces.dto.PlayerCardResponse;
import com.fco.platform.player.interfaces.dto.PlayerDetailResponse;
import com.fco.platform.common.dto.resp.PageResponse;

public interface IPlayerService {
    PageResponse<PlayerCardResponse> getPlayers(
            String keyword,
            String seasonCode,
            Long nationId,
            String position,
            Long minPrice,
            Long maxPrice,
            int page,
            int size
    );
    PlayerDetailResponse getPlayerDetail(Long id);
}
