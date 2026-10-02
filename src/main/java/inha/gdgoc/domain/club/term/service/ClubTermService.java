package inha.gdgoc.domain.club.term.service;

import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.repository.ClubTermRepository;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.term.dto.ClubTermCreateRequest;
import inha.gdgoc.domain.club.term.dto.ClubTermResponse;
import inha.gdgoc.domain.club.term.dto.ClubTermUpdateRequest;
import inha.gdgoc.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기수 설정 (이름·참석 비율). 기수에는 날짜가 없으므로 「최신순」 = 나중에 만든 순이다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTermService {

  private final ClubTermRepository clubTermRepository;

  public List<ClubTermResponse> findAll() {
    return clubTermRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
        .map(ClubTermResponse::from)
        .toList();
  }

  @Transactional
  public ClubTermResponse create(ClubTermCreateRequest req) {
    ClubTerm term =
        clubTermRepository.save(ClubTerm.create(req.name().strip(), req.attendanceRatio()));
    return ClubTermResponse.from(term);
  }

  @Transactional
  public ClubTermResponse update(Long termId, ClubTermUpdateRequest req) {
    ClubTerm term =
        clubTermRepository
            .findById(termId)
            .orElseThrow(() -> new BusinessException(ClubErrorCode.TERM_NOT_FOUND));
    term.update(req.name() == null ? null : req.name().strip(), req.attendanceRatio());
    return ClubTermResponse.from(term);
  }
}
