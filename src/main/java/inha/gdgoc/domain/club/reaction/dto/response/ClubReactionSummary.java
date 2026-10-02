package inha.gdgoc.domain.club.reaction.dto.response;

/** 목록 카드 한 장의 반응 수. B 의 피드가 {@code ClubReactionService#summarize} 로 받아 붙인다. */
public record ClubReactionSummary(long likeCount, long commentCount, boolean likedByMe) {

  public static final ClubReactionSummary EMPTY = new ClubReactionSummary(0, 0, false);
}
