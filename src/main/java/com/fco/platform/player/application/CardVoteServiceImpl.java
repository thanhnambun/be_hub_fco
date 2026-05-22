package com.fco.platform.player.application;

import com.fco.platform.auth.domain.User;
import com.fco.platform.auth.infrastructure.persistence.IUserRepository;
import com.fco.platform.card.domain.FcoCardVote;
import com.fco.platform.card.domain.FcoCardVote.VoteType;
import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.card.infrastructure.persistence.IFcoCardVoteRepository;
import com.fco.platform.card.infrastructure.persistence.IPlayerCardRepository;
import com.fco.platform.common.application.RedisService;
import com.fco.platform.common.exception.BusinessException;
import com.fco.platform.common.exception.ErrorCode;
import com.fco.platform.common.exception.TooManyRequestsException;
import com.fco.platform.player.interfaces.dto.ReviewDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

/**
 * Service xử lý bình chọn NGON/PHE cấp thẻ cầu thủ.
 *
 * <h3>Transaction Safety & Abuse Prevention</h3>
 * Toggle vote là một thao tác read-then-write điển hình, dễ gặp race condition
 * khi user spam click. Chiến lược:
 * <ul>
 *   <li>Isolation = READ_COMMITTED (tránh phantom read)</li>
 *   <li>Bắt {@link DataIntegrityViolationException} xử lý duplicate PK do race condition.</li>
 *   <li>Rate Limiting: 5 requests / 10 seconds sử dụng Redis.</li>
 *   <li>Anti-spam Telemetry: Cảnh báo hành vi bất thường (thay đổi vote > 100 lần / 1 phút).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardVoteServiceImpl implements ICardVoteService {

    private final IFcoCardVoteRepository cardVoteRepo;
    private final IPlayerCardRepository  cardRepo;
    private final IUserRepository        userRepo;
    private final RedisService           redisService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReviewDtos.CardVoteResponse toggleCardVote(Long cardId,
                                                      ReviewDtos.CardVoteRequest request,
                                                      Long userId) {
        // ── 1. Rate Limiting (5 requests / 10 seconds) ──────────────────────────
        String rateLimitKey = "RATE_LIMIT:CARD_VOTE:" + userId;
        Long rateCount = redisService.incrementCounter(rateLimitKey);
        if (rateCount != null && rateCount == 1L) {
            redisService.expireCounter(rateLimitKey, Duration.ofSeconds(10));
        }
        if (rateCount != null && rateCount > 5) {
            log.warn("[CARD_VOTE_RATE_LIMIT] userId={} cardId={} count={}", userId, cardId, rateCount);
            throw new TooManyRequestsException("Bạn đang thao tác quá nhanh. Vui lòng thử lại sau 10 giây.");
        }

        // ── 2. Validate voteType ──────────────────────────────────────────────
        VoteType newType;
        try {
            newType = VoteType.valueOf(request.getVoteType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Loại vote không hợp lệ. Chỉ chấp nhận: NGON, PHE", ErrorCode.VAL_BAD_REQUEST);
        }

        // ── 3. Load entities ──────────────────────────────────────────────────
        PlayerCard card = cardRepo.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng", ErrorCode.USER_NOT_FOUND));

        // ── 4. Toggle logic với structured logging và telemetry ────────────────
        Optional<FcoCardVote> existing = cardVoteRepo.findByCardIdAndUserId(cardId, userId);
        String previousVote = existing.map(v -> v.getVoteType().name()).orElse(null);

        // Telemetry: Theo dõi hành vi đổi vote liên tục (chỉ tính khi thay đổi thực tế)
        boolean isChange = existing.isEmpty() || existing.get().getVoteType() != newType;
        if (isChange) {
            String telemetryKey = "TELEMETRY:CARD_VOTE_CHANGES:" + userId;
            Long changeCount = redisService.incrementCounter(telemetryKey);
            if (changeCount != null && changeCount == 1L) {
                redisService.expireCounter(telemetryKey, Duration.ofSeconds(60));
            }
            if (changeCount != null && changeCount > 100) {
                log.warn("[SUSPICIOUS_BEHAVIOR] User {} is spamming vote changes! Count={} in the last minute on card {}",
                        userId, changeCount, cardId);
            }
        }

        try {
            if (existing.isPresent()) {
                FcoCardVote existingVote = existing.get();
                if (existingVote.getVoteType() == newType) {
                    // Nhấn cùng loại → toggle off
                    cardVoteRepo.deleteByCardIdAndUserId(cardId, userId);
                    log.info("[CARD_VOTE] action=CANCEL cardId={} userId={} vote={}", cardId, userId, newType);
                } else {
                    // Nhấn loại khác → đổi vote
                    existingVote.setVoteType(newType);
                    cardVoteRepo.saveAndFlush(existingVote);
                    log.info("[CARD_VOTE] action=CHANGE cardId={} userId={} from={} to={}", cardId, userId, previousVote, newType);
                }
            } else {
                // Chưa có vote → tạo mới
                FcoCardVote vote = FcoCardVote.builder()
                        .card(card)
                        .user(user)
                        .voteType(newType)
                        .build();
                cardVoteRepo.saveAndFlush(vote);
                log.info("[CARD_VOTE] action=CREATE cardId={} userId={} vote={}", cardId, userId, newType);
            }
        } catch (DataIntegrityViolationException ex) {
            // Race condition: 2 request đồng thời đến cùng lúc.
            // Request thứ 2 bị duplicate PK → KHÔNG throw 500, trả về trạng thái DB hiện tại.
            log.warn("[CARD_VOTE] race_condition cardId={} userId={} voteType={} — returning current state",
                    cardId, userId, newType);
        }

        // ── 5. Trả về trạng thái mới nhất (đọc sau commit) ───────────────────
        long ngon = cardVoteRepo.countByCardIdAndVoteType(cardId, VoteType.NGON);
        long phe  = cardVoteRepo.countByCardIdAndVoteType(cardId, VoteType.PHE);
        String currentVote = cardVoteRepo.findByCardIdAndUserId(cardId, userId)
                .map(v -> v.getVoteType().name())
                .orElse(null);

        log.debug("[CARD_VOTE] result cardId={} ngon={} phe={} currentVote={}", cardId, ngon, phe, currentVote);

        return ReviewDtos.CardVoteResponse.builder()
                .cardId(cardId)
                .ngonCount(ngon)
                .pheCount(phe)
                .currentUserVote(currentVote)
                .build();
    }
}
