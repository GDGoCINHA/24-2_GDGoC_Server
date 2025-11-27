package inha.gdgoc.domain.manito.repository;

import inha.gdgoc.domain.manito.entity.ManitoAssignment;
import inha.gdgoc.domain.manito.entity.ManitoSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManitoAssignmentRepository extends JpaRepository<ManitoAssignment, Long> {

    /**
     * 특정 세션 + 학번(studentId)
     * - verify API에서 사용
     */
    Optional<ManitoAssignment> findBySessionAndStudentId(ManitoSession session, String studentId);

    /**
     * 세션 코드 + 학번 단일 조회
     * - sessionRepo 없이 바로 조회 가능
     */
    Optional<ManitoAssignment> findBySession_CodeAndStudentId(String sessionCode, String studentId);

    /**
     * 특정 세션 전체 조회
     * - admin 화면 (일괄 조회)
     */
    List<ManitoAssignment> findBySession(ManitoSession session);

    /**
     * 특정 세션 내 모든 assignment 삭제
     * - 세션 재생성, 재업로드 시 유용
     */
    Long deleteBySession(ManitoSession session);

    /**
     * 특정 세션의 특정 studentId가 존재하는지 확인
     * - 중복 업로드 검증
     */
    boolean existsBySessionAndStudentId(ManitoSession session, String studentId);
}