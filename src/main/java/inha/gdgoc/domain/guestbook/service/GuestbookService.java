package inha.gdgoc.domain.guestbook.service;

import inha.gdgoc.domain.guestbook.dto.request.LuckyDrawRequest;
import inha.gdgoc.domain.guestbook.dto.response.GuestbookEntryResponse;
import inha.gdgoc.domain.guestbook.dto.response.LuckyDrawWinnerResponse;
import inha.gdgoc.domain.guestbook.entity.GuestbookEntry;
import inha.gdgoc.domain.guestbook.repository.GuestbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class GuestbookService {

    private final GuestbookRepository repo;

    /* ===== 등록 ===== */
    public GuestbookEntryResponse register(String wristbandSerial, String name) {
        wristbandSerial = wristbandSerial == null ? "" : wristbandSerial.trim();
        name = name == null ? "" : name.trim();

        if (wristbandSerial.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("wristbandSerial and name are required");
        }
        if (repo.existsByWristbandSerial(wristbandSerial)) {
            throw new DuplicateWristbandSerialException();
        }

        GuestbookEntry saved = repo.save(new GuestbookEntry(wristbandSerial, name));
        return GuestbookEntryResponse.from(saved);
    }

    /* ===== 목록 ===== */
    @Transactional(readOnly = true)
    public List<GuestbookEntryResponse> listEntries() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(GuestbookEntryResponse::from).toList();
    }

    /* ===== 단건 ===== */
    @Transactional(readOnly = true)
    public GuestbookEntryResponse get(Long id) {
        return repo.findById(id).map(GuestbookEntryResponse::from).orElseThrow(NotFoundException::new);
    }

    /* ===== 삭제 ===== */
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /* ===== 추첨 ===== */
    public List<LuckyDrawWinnerResponse> draw(LuckyDrawRequest req) {
        int count = req.count();

        List<GuestbookEntry> pool = repo.findAllByWonAtIsNullForUpdate();
        if (pool.size() < count) {
            throw new NoCandidatesException();
        }

        Collections.shuffle(pool);

        List<LuckyDrawWinnerResponse> winners = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GuestbookEntry e = pool.get(i);
            e.markWon();
            winners.add(LuckyDrawWinnerResponse.from(e));
        }
        return winners;
    }

    /* ===== 당첨자 목록 ===== */
    @Transactional(readOnly = true)
    public List<LuckyDrawWinnerResponse> listWinners() {
        return repo.findAllByWonAtIsNotNullOrderByWonAtAsc().stream().map(LuckyDrawWinnerResponse::from).toList();
    }

    /* ===== 리셋 ===== */
    public long resetWinners() {
        return repo.clearAllWinners();
    }

    /* ===== exceptions ===== */
    public static class DuplicateWristbandSerialException extends RuntimeException {

    }

    public static class NoCandidatesException extends RuntimeException {

    }

    public static class NotFoundException extends RuntimeException {

    }
}
