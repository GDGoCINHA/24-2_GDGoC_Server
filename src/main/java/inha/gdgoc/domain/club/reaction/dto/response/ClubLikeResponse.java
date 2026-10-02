package inha.gdgoc.domain.club.reaction.dto.response;

/** 좋아요·취소 뒤의 상태. 화면이 숫자를 다시 받아 그리게 한다. */
public record ClubLikeResponse(boolean liked, long likeCount) {}
