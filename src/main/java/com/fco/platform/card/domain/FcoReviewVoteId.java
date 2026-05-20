package com.fco.platform.card.domain;

import java.io.Serializable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FcoReviewVoteId implements Serializable {
    private Long review;
    private Long user;
}
