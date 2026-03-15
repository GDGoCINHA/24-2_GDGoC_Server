package inha.gdgoc.domain.game.service;

import inha.gdgoc.domain.game.dto.request.BingoBoardUpdateRequest;
import inha.gdgoc.domain.game.dto.response.BingoBoardResponse;
import inha.gdgoc.domain.game.entity.BingoBoard;
import inha.gdgoc.domain.game.entity.BingoInteraction;
import inha.gdgoc.domain.game.repository.BingoBoardRepository;
import inha.gdgoc.domain.game.repository.BingoInteractionRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
public class BingoBoardService {

    private static final int TEAM_COUNT = 10;
    private static final int CELL_COUNT = 16;
    private static final String EMPTY_MARK = "empty";
    private static final String CHECKED_MARK = "x";

    private final BingoBoardRepository bingoBoardRepository;
    private final BingoInteractionRepository bingoInteractionRepository;

    public List<BingoBoardResponse> findAllBoards() {
        return buildRankedResponses(loadBoardStates());
    }

    public BingoBoardResponse findBoard(Integer teamNumber) {
        validateTeamNumber(teamNumber);

        List<BoardState> boardStates = loadBoardStates();
        return buildRankedResponses(boardStates).stream()
                .filter(response -> response.getTeamNumber().equals(teamNumber))
                .findFirst()
                .orElseGet(() -> new BingoBoardResponse(teamNumber, createEmptyMarks(), 0, teamNumber));
    }

    @Transactional
    public BingoBoardResponse updateBoard(Integer teamNumber, BingoBoardUpdateRequest request) {
        validateTeamNumber(teamNumber);

        List<String> nextMarks = normalizeMarks(request.getMarks());
        BingoBoard bingoBoard = bingoBoardRepository.findByTeamNumber(teamNumber)
                .orElseGet(() -> BingoBoard.builder()
                        .teamNumber(teamNumber)
                        .marks(serializeMarks(createEmptyMarks()))
                        .build());

        List<String> previousMarks = deserializeMarks(bingoBoard.getMarks());
        saveInteractions(teamNumber, previousMarks, nextMarks);

        bingoBoard.updateMarks(serializeMarks(nextMarks));
        bingoBoardRepository.save(bingoBoard);

        return findBoard(teamNumber);
    }

    private List<BoardState> loadBoardStates() {
        List<BingoBoard> savedBoards = bingoBoardRepository.findAllByOrderByTeamNumberAsc();

        return IntStream.rangeClosed(1, TEAM_COUNT)
                .mapToObj(teamNumber -> savedBoards.stream()
                        .filter(board -> board.getTeamNumber().equals(teamNumber))
                        .findFirst()
                        .map(board -> new BoardState(teamNumber, deserializeMarks(board.getMarks())))
                        .orElseGet(() -> new BoardState(teamNumber, createEmptyMarks())))
                .toList();
    }

    private List<BingoBoardResponse> buildRankedResponses(List<BoardState> boardStates) {
        List<BoardState> sortedStates = new ArrayList<>(boardStates);
        sortedStates.sort(Comparator.comparingInt(BoardState::checkedCount).reversed()
                .thenComparingInt(BoardState::teamNumber));

        List<BingoBoardResponse> responses = new ArrayList<>();
        Integer previousCheckedCount = null;
        int previousRank = 0;

        for (int index = 0; index < sortedStates.size(); index++) {
            BoardState state = sortedStates.get(index);
            int rank = previousCheckedCount != null && previousCheckedCount == state.checkedCount()
                    ? previousRank
                    : index + 1;

            responses.add(new BingoBoardResponse(
                    state.teamNumber(),
                    state.marks(),
                    state.checkedCount(),
                    rank
            ));

            previousCheckedCount = state.checkedCount();
            previousRank = rank;
        }

        return responses;
    }

    private void saveInteractions(Integer teamNumber, List<String> previousMarks, List<String> nextMarks) {
        List<BingoInteraction> interactions = IntStream.range(0, CELL_COUNT)
                .filter(index -> !previousMarks.get(index).equals(nextMarks.get(index)))
                .mapToObj(index -> BingoInteraction.builder()
                        .teamNumber(teamNumber)
                        .cellIndex(index + 1)
                        .mark(nextMarks.get(index))
                        .build())
                .toList();

        if (!interactions.isEmpty()) {
            bingoInteractionRepository.saveAll(interactions);
        }
    }

    private List<String> normalizeMarks(List<String> marks) {
        if (marks == null || marks.size() != CELL_COUNT) {
            return createEmptyMarks();
        }

        return marks.stream()
                .map(mark -> CHECKED_MARK.equals(mark) ? CHECKED_MARK : EMPTY_MARK)
                .toList();
    }

    private List<String> deserializeMarks(String marks) {
        if (marks == null || marks.isBlank()) {
            return createEmptyMarks();
        }

        String[] split = marks.split(",", -1);
        if (split.length != CELL_COUNT) {
            return createEmptyMarks();
        }

        List<String> normalizedMarks = new ArrayList<>();
        for (String mark : split) {
            normalizedMarks.add(CHECKED_MARK.equals(mark) ? CHECKED_MARK : EMPTY_MARK);
        }
        return normalizedMarks;
    }

    private String serializeMarks(List<String> marks) {
        return String.join(",", normalizeMarks(marks));
    }

    private List<String> createEmptyMarks() {
        return IntStream.range(0, CELL_COUNT)
                .mapToObj(index -> EMPTY_MARK)
                .toList();
    }

    private void validateTeamNumber(Integer teamNumber) {
        if (teamNumber == null || teamNumber < 1 || teamNumber > TEAM_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 팀 번호입니다.");
        }
    }

    private record BoardState(Integer teamNumber, List<String> marks) {
        private int checkedCount() {
            return (int) marks.stream().filter(CHECKED_MARK::equals).count();
        }
    }
}
