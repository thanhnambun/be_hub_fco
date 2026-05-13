package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.PageResponse;
import com.ra.base_spring_boot.dto.resp.PlayerCardResponse;
import com.ra.base_spring_boot.mapper.PlayerCardMapper;
import com.ra.base_spring_boot.model.fco.PlayerCard;
import com.ra.base_spring_boot.repository.IPlayerCardRepository;
import com.ra.base_spring_boot.services.IPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements IPlayerService {
    private final IPlayerCardRepository playerCardRepository;
    private final PlayerCardMapper playerCardMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlayerCardResponse> getPlayers(String keyword, String seasonCode, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);

        Page<PlayerCard> cardPage = playerCardRepository.search(keyword, seasonCode, pageable);
        List<PlayerCardResponse> items = cardPage.getContent().stream().map(playerCardMapper::toResponse).toList();

        return PageResponse.<PlayerCardResponse>builder()
                .items(items)
                .page(cardPage.getNumber())
                .size(cardPage.getSize())
                .totalItems(cardPage.getTotalElements())
                .totalPages(cardPage.getTotalPages())
                .hasNext(cardPage.hasNext())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.ra.base_spring_boot.dto.resp.PlayerDetailResponse getPlayerDetail(Long id) {
        PlayerCard card = playerCardRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        var p = card.getPlayer();
        
        var clubs = p.getTeamColors().stream()
                .map(c -> com.ra.base_spring_boot.dto.resp.PlayerDetailResponse.ClubResponse.builder()
                        .clubName(c.getClubName())
                        .clubSlug(c.getClubSlug())
                        .clubFifaaddictId(c.getFifaaddictId())
                        .crestUrl(c.getCrestUrl())
                        .build())
                .collect(Collectors.toList());

        var traits = card.getTraits().stream()
                .map(t -> com.ra.base_spring_boot.dto.resp.PlayerDetailResponse.TraitResponse.builder()
                        .traitCode(t.getTraitCode())
                        .traitName(t.getTraitName())
                        .description(t.getDescription())
                        .iconId(t.getIconId())
                        .iconUrl(t.getIconUrl())
                        .build())
                .collect(Collectors.toList());

        var prices = card.getPrices().stream()
                .sorted(Comparator.comparing(com.ra.base_spring_boot.model.fco.FcoCardPrice::getGrade))
                .map(pr -> com.ra.base_spring_boot.dto.resp.PlayerDetailResponse.PriceResponse.builder()
                        .grade(pr.getGrade())
                        .priceBp(pr.getPriceBp())
                        .priceRaw(pr.getPriceRaw())
                        .priceDate(pr.getPriceDate())
                        .build())
                .collect(Collectors.toList());

        return com.ra.base_spring_boot.dto.resp.PlayerDetailResponse.builder()
                .id(card.getId())
                .externalId(p.getExternalId())
                .playerName(p.getPlayerName())
                .seasonCode(card.getSeason().getSeasonCode())
                .enhanceLevel(card.getEnhanceLevel())
                .ovr(card.getOvr())
                .salary(card.getSalary())
                .preferredPosition(card.getPreferredPosition())
                .marketPriceBp(card.getMarketPriceBp())
                .imageUrl(card.getImageUrl())
                // Core Stats
                .pace(card.getPace())
                .shooting(card.getShooting())
                .passing(card.getPassing())
                .dribbling(card.getDribbling())
                .defending(card.getDefending())
                .physicality(card.getPhysicality())
                // Details
                .liveperf(card.getLiveperf())
                .hasLivePerf(card.getLiveperf() != null && card.getLiveperf() > 0)
                .skillLevel(card.getSkillLevel())
                .secondaryPosition(card.getSecondaryPosition())
                .workerateAtt(card.getWorkerateAtt())
                .workerateDef(card.getWorkerateDef())
                .bodytype(card.getBodytype())
                .reputation(card.getReputation())
                .priceUpdatedAt(card.getPriceUpdatedAt())
                .ovrByPosJson(card.getOvrByPosJson())
                // Bio
                .height(p.getHeight())
                .weight(p.getWeight())
                .birthdate(p.getBirthdate())
                .preferredFoot(p.getPreferredFoot())
                .weakFoot(p.getWeakFoot())
                .nationName(p.getNationName())
                .nationSlug(p.getNation() != null ? p.getNation().getNationSlug() : null)
                .leagueName(p.getLeagueName())
                .leagueSlug(p.getLeague() != null ? p.getLeague().getLeagueSlug() : null)
                // Collections
                .clubs(clubs)
                .traits(traits)
                .prices(prices)
                .build();
    }
}
