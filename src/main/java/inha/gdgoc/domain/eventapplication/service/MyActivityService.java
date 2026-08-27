package inha.gdgoc.domain.eventapplication.service;

import inha.gdgoc.domain.eventapplication.dto.response.MyActivityResponse;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이페이지의 활동 이력.
 *
 * <p>지금은 행사 참여만 담지만 나중에 스터디·정기모임을 같은 목록에 합칠 수 있도록 경로와 이름을 넓게 잡았다. 취소한 신청은 이력이 아니므로 뺀다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyActivityService {

  private final EventApplicationRepository applicationRepository;

  public List<MyActivityResponse> listMyEventActivities(Long userId) {
    return applicationRepository.findMyActivities(userId, ApplicationStatus.APPLIED).stream()
        .map(MyActivityResponse::from)
        .toList();
  }
}
