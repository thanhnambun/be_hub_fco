package com.fco.platform.card.domain;

import java.io.Serializable;
import lombok.*;

/**
 * Composite primary key cho FcoCardVote.
 * card → card_id (FK fco_player_cards), user → user_id (FK users).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FcoCardVoteId implements Serializable {
    private Long card;
    private Long user;
}
