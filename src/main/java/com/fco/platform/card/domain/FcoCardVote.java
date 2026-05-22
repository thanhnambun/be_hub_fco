package com.fco.platform.card.domain;

import com.fco.platform.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bình chọn NGON / PHẾ của người dùng dành cho một thẻ cầu thủ.
 * Composite PK (card_id, user_id) – mỗi user chỉ có 1 vote cho 1 thẻ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fco_card_votes")
@IdClass(FcoCardVoteId.class)
public class FcoCardVote {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private PlayerCard card;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 10)
    private VoteType voteType;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum VoteType {
        NGON, PHE
    }
}
