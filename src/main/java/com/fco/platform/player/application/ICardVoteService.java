package com.fco.platform.player.application;

import com.fco.platform.player.interfaces.dto.ReviewDtos;

public interface ICardVoteService {

    /**
     * Toggle bình chọn NGON / PHE của user cho một thẻ cầu thủ.
     * - Nhấn cùng loại → hủy vote.
     * - Nhấn loại khác → đổi vote.
     * - Chưa có vote    → tạo mới.
     *
     * @param cardId   ID thẻ cầu thủ
     * @param request  DTO chứa voteType ("NGON" | "PHE")
     * @param userId   ID người dùng đang thao tác
     * @return CardVoteResponse kèm số đếm mới nhất và trạng thái vote của user
     */
    ReviewDtos.CardVoteResponse toggleCardVote(Long cardId, ReviewDtos.CardVoteRequest request, Long userId);
}
