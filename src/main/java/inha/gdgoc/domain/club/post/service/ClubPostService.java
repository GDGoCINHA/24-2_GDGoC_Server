package inha.gdgoc.domain.club.post.service;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.post.dto.request.ClubPostRequest;
import inha.gdgoc.domain.club.post.entity.ClubPost;
import inha.gdgoc.domain.club.post.exception.ClubPostErrorCode;
import inha.gdgoc.domain.club.post.repository.ClubPostRepository;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 일반 게시글(공지·질문·후기·자료). 완주에 반영하지 않는다.
 *
 * <p>쓰기는 팀 멤버, 고치기는 작성자, 지우기는 작성자·리더·운영진. 지워도 행은 남는다 — 좋아요·댓글이 가리킨다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPostService {

  private final ClubAccessService clubAccessService;
  private final ClubPostRepository clubPostRepository;
  private final UserRepository userRepository;

  @Transactional
  public Long create(Long clubId, Long userId, ClubPostRequest req) {
    clubAccessService.requireActiveMember(clubId, userId);
    Club club = clubAccessService.getClub(clubId);
    ClubPost post =
        ClubPost.create(
            club,
            userRepository.getReferenceById(userId),
            req.category(),
            req.content().trim(),
            imagesOf(req));
    return clubPostRepository.save(post).getId();
  }

  @Transactional
  public void update(Long clubId, Long postId, Long userId, ClubPostRequest req) {
    ClubPost post = findInClub(clubId, postId);
    if (!post.isWrittenBy(userId)) {
      throw new BusinessException(ClubPostErrorCode.POST_EDIT_FORBIDDEN);
    }
    post.update(req.category(), req.content().trim(), imagesOf(req));
  }

  @Transactional
  public void delete(Long clubId, Long postId, Long userId, boolean staff) {
    ClubPost post = findInClub(clubId, postId);
    if (!post.isWrittenBy(userId) && !staff && !clubAccessService.isLeader(clubId, userId)) {
      throw new BusinessException(ClubPostErrorCode.POST_DELETE_FORBIDDEN);
    }
    post.softDelete(Instant.now());
  }

  /** 지운 글과 다른 소모임의 글은 없는 것으로 본다. */
  private ClubPost findInClub(Long clubId, Long postId) {
    return clubPostRepository
        .findById(postId)
        .filter(p -> !p.isDeleted() && p.getClub().getId().equals(clubId))
        .orElseThrow(() -> new BusinessException(ClubPostErrorCode.POST_NOT_FOUND));
  }

  private static List<String> imagesOf(ClubPostRequest req) {
    return req.imageUrls() == null ? List.of() : List.copyOf(req.imageUrls());
  }
}
