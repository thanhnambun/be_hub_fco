package com.fco.platform.player.application;

import com.fco.platform.auth.domain.User;
import com.fco.platform.common.exception.BusinessException;
import com.fco.platform.common.exception.ErrorCode;
import com.fco.platform.player.interfaces.dto.PlayerCardResponse;
import com.fco.platform.player.interfaces.dto.PlayerDetailResponse;
import com.fco.platform.player.interfaces.mapper.PlayerDetailMapper;
import com.fco.platform.card.domain.FcoCardVote;
import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.card.infrastructure.persistence.IFcoCardVoteRepository;
import com.fco.platform.card.infrastructure.persistence.IPlayerCardRepository;
import com.fco.platform.card.interfaces.mapper.PlayerCardMapper;
import com.fco.platform.common.dto.resp.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements IPlayerService {
    private final IPlayerCardRepository  playerCardRepository;
    private final PlayerCardMapper       playerCardMapper;
    private final PlayerDetailMapper     playerDetailMapper;
    private final IFcoCardVoteRepository cardVoteRepo;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlayerCardResponse> getPlayers(
            String keyword,
            String seasonCode,
            Long nationId,
            String position,
            Long minPrice,
            Long maxPrice,
            int page,
            int size
    ) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new BusinessException(ErrorCode.MARKET_QUOTE_INVALID);
        }

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);

        Page<PlayerCard> cardPage = playerCardRepository.search(
                keyword,
                seasonCode,
                nationId,
                position,
                minPrice,
                maxPrice,
                pageable
        );
        List<PlayerCardResponse> items = cardPage.getContent().stream()
                .map(playerCardMapper::toResponse)
                .toList();

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
    public PlayerDetailResponse getPlayerDetail(Long id) {
        PlayerCard card = playerCardRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        PlayerDetailResponse resp = playerDetailMapper.toDetailResponse(card);

        // Inject card-level vote counts
        resp.setNgonCount(cardVoteRepo.countByCardIdAndVoteType(id, FcoCardVote.VoteType.NGON));
        resp.setPheCount(cardVoteRepo.countByCardIdAndVoteType(id, FcoCardVote.VoteType.PHE));

        // Inject current user vote nếu đã đăng nhập
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User currentUser) {
            String currentVote = cardVoteRepo.findByCardIdAndUserId(id, currentUser.getId())
                    .map(v -> v.getVoteType().name())
                    .orElse(null);
            resp.setCurrentUserVote(currentVote);
        }

        return resp;
    }
}
