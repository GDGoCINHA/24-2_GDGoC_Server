package inha.gdgoc.domain.manito.service;

import inha.gdgoc.domain.manito.dto.request.ManitoSessionCreateRequest;
import inha.gdgoc.domain.manito.dto.response.ManitoSessionResponse;
import inha.gdgoc.domain.manito.entity.ManitoAssignment;
import inha.gdgoc.domain.manito.entity.ManitoSession;
import inha.gdgoc.domain.manito.repository.ManitoAssignmentRepository;
import inha.gdgoc.domain.manito.repository.ManitoSessionRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManitoAdminService {

    private final ManitoSessionRepository sessionRepository;
    private final ManitoAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 간단 CSV escape (콤마/따옴표/줄바꿈 포함 시 따옴표 감싸기)
     */
    private static String escapeCsv(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!needQuote) return s;
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    private static List<Integer> computeGroupSizes(int n) {
        if (n < 5) {
            throw new IllegalArgumentException("n must be >= 5");
        }

        // 처음엔 전체를 하나의 그룹으로 두고,
        // 크기가 10 이상인 그룹은 [5, 나머지]로 계속 쪼갠다.
        // 그러면 모든 그룹이 5~9명이 된다.
        List<Integer> groups = new ArrayList<>();
        groups.add(n);

        while (true) {
            int idx = -1;
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i) >= 10) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1) break; // 더 이상 쪼갤 그룹 없음

            int size = groups.get(idx);
            groups.remove(idx);
            groups.add(idx, size - 5);
            groups.add(idx, 5);
        }

        return groups;
    }

    /**
     * CSV 헤더: studentId,name,pin
     */
    @Transactional
    public void importParticipantsCsv(String sessionCode, MultipartFile file) {
        ManitoSession session = sessionRepository.findByCode(sessionCode)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND, "세션 코드를 찾을 수 없습니다: " + sessionCode));

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "비어 있는 CSV 파일입니다.");
            }

            // 1) 구분자 추론 (탭 우선)
            String delimiter = header.contains("\t") ? "\t" : ",";

            // 2) 헤더 컬럼 개수 확인
            String[] headerCols = header.split(delimiter, -1);
            //   - 4개 이상: [타임스탬프, 학번, 이름, PIN, ...]
            //   - 3개:      [studentId, name, pin] 형식으로 이미 전처리된 경우
            int studentIdx;
            int nameIdx;
            int pinIdx;

            if (headerCols.length >= 4) {
                // 구글폼 원본처럼 타임스탬프 포함된 케이스
                studentIdx = 1;
                nameIdx = 2;
                pinIdx = 3;
            } else if (headerCols.length == 3) {
                // 사전에 [studentId,name,pin] 으로 정리된 파일
                studentIdx = 0;
                nameIdx = 1;
                pinIdx = 2;
            } else {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "지원하지 않는 CSV 헤더 형식입니다. (컬럼 수: " + headerCols.length + ")");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] cols = line.split(delimiter, -1);
                if (cols.length <= pinIdx) {
                    throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "CSV 컬럼 수가 부족합니다: " + line);
                }

                String studentId = cols[studentIdx].trim();
                String name = cols[nameIdx].trim();
                String pinPlain = cols[pinIdx].trim();

                // 혹시 이상한 문자 섞였으면 (예: 김가은`)
                name = name.replace("`", "").trim();

                if (studentId.isEmpty() || name.isEmpty() || pinPlain.isEmpty()) {
                    // 비어 있으면 스킵 (원하면 여기서 에러 던져도 됨)
                    continue;
                }

                // 여기서 PIN 길이 정책은 네가 선택
                // - 그대로 쓰기
                // - 숫자만 남기고 4자리 zero padding 하기 등
                // ex) 숫자만 추출:
                // pinPlain = pinPlain.replaceAll("\\D", "");
                // if (pinPlain.length() < 4) { ... }

                String pinHash = passwordEncoder.encode(pinPlain);

                var existingOpt = assignmentRepository.findBySessionAndStudentId(session, studentId);

                if (existingOpt.isPresent()) {
                    ManitoAssignment existing = existingOpt.get();
                    existing.changeName(name);
                    existing.changePinHash(pinHash);
                } else {
                    ManitoAssignment assignment = ManitoAssignment.builder()
                            .session(session)
                            .studentId(studentId)
                            .name(name)
                            .encryptedManitto(null) // 나중에 upload-encrypted 로 채움
                            .pinHash(pinHash)
                            .build();
                    assignmentRepository.save(assignment);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "CSV 업로드 처리 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    public String buildAssignmentsCsv(String sessionCode) {
        ManitoSession session = sessionRepository.findByCode(sessionCode)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND, "세션 코드를 찾을 수 없습니다: " + sessionCode));

        // 참가자 전체 조회
        var participants = assignmentRepository.findBySession(session)
                .stream()
                .sorted(Comparator.comparing(ManitoAssignment::getStudentId)) // 학번 기준 정렬(선택)
                .toList();

        int n = participants.size();
        if (n < 5) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "매칭을 생성하기 위해서는 최소 5명 이상의 참가자가 필요합니다.");
        }

        // 랜덤성 확보: 참가자 순서를 섞어놓고, 그 위에서 서킷 분할
        List<ManitoAssignment> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled); // 필요하면 SecureRandom 사용해도 됨

        // 서킷 크기 리스트 계산 (각 서킷 최소 5명)
        List<Integer> groupSizes = computeGroupSizes(n);

        StringBuilder sb = new StringBuilder();
        // 매칭용 헤더 (암호화 전, 순수 매칭 정보)
        sb.append("giverStudentId,giverName,receiverStudentId,receiverName\n");

        int index = 0;
        for (int size : groupSizes) {
            List<ManitoAssignment> group = shuffled.subList(index, index + size);
            index += size;

            // 하나의 서킷: group[i] -> group[(i+1) % size]
            for (int i = 0; i < size; i++) {
                ManitoAssignment giver = group.get(i);
                ManitoAssignment receiver = group.get((i + 1) % size);

                sb.append(escapeCsv(giver.getStudentId()))
                        .append(',')
                        .append(escapeCsv(giver.getName()))
                        .append(',')
                        .append(escapeCsv(receiver.getStudentId()))
                        .append(',')
                        .append(escapeCsv(receiver.getName()))
                        .append('\n');
            }
        }

        return new String(sb.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Transactional
    public void importEncryptedCsv(String sessionCode, MultipartFile file) {
        ManitoSession session = sessionRepository.findByCode(sessionCode)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND, "세션 코드를 찾을 수 없습니다: " + sessionCode));

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "비어 있는 CSV 파일입니다.");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 2) {
                    throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "CSV 컬럼 수가 부족합니다: " + line);
                }

                String studentId = cols[0].trim();
                String encryptedManitto = cols[1].trim();

                if (studentId.isEmpty() || encryptedManitto.isEmpty()) {
                    // 비어 있는 줄은 그냥 스킵
                    continue;
                }

                ManitoAssignment assignment = assignmentRepository.findBySessionAndStudentId(session, studentId)
                        .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND, "해당 학번에 대한 참가자 정보가 없습니다: " + studentId));

                assignment.changeEncryptedManitto(encryptedManitto);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "암호문 CSV 업로드 처리 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    public ManitoSessionResponse createSession(ManitoSessionCreateRequest req) {
        String code = req.code().trim();

        if (sessionRepository.existsByCode(code)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "이미 존재하는 세션 코드입니다: " + code);
        }

        ManitoSession session = ManitoSession.builder().code(code).title(req.title().trim()).build();

        sessionRepository.save(session);
        return ManitoSessionResponse.from(session);
    }

    @Transactional
    public List<ManitoSessionResponse> listSessions() {
        return sessionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(ManitoSessionResponse::from)
                .toList();
    }
}