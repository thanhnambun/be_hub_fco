package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.PageResponse;
import com.ra.base_spring_boot.dto.resp.PlayerCardResponse;

public interface IPlayerService {
    PageResponse<PlayerCardResponse> getPlayers(String keyword, String seasonCode, int page, int size);
    com.ra.base_spring_boot.dto.resp.PlayerDetailResponse getPlayerDetail(Long id);
}
