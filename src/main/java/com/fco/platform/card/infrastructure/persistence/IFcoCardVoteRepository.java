package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoCardVote;
import com.fco.platform.card.domain.FcoCardVoteId;
import com.fco.platform.card.domain.FcoCardVote.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface IFcoCardVoteRepository extends JpaRepository<FcoCardVote, FcoCardVoteId> {

    /** Đếm số lượt vote NGON hoặc PHE cho một thẻ cầu thủ cụ thể. */
    @Query("SELECT COUNT(v) FROM FcoCardVote v WHERE v.card.id = :cardId AND v.voteType = :voteType")
    long countByCardIdAndVoteType(@Param("cardId") Long cardId, @Param("voteType") VoteType voteType);

    /** Tìm vote hiện tại của user cho một thẻ cầu thủ (để toggle). */
    @Query("SELECT v FROM FcoCardVote v WHERE v.card.id = :cardId AND v.user.id = :userId")
    Optional<FcoCardVote> findByCardIdAndUserId(@Param("cardId") Long cardId, @Param("userId") Long userId);

    /** Xóa vote của user cho thẻ cầu thủ (dùng khi toggle off). */
    @Modifying
    @Transactional
    @Query("DELETE FROM FcoCardVote v WHERE v.card.id = :cardId AND v.user.id = :userId")
    void deleteByCardIdAndUserId(@Param("cardId") Long cardId, @Param("userId") Long userId);
}
