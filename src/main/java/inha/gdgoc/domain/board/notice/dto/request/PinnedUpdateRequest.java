package inha.gdgoc.domain.board.notice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PinnedUpdateRequest(
    @NotNull @Size(max = 3) List<UUID> pinnedArticleIds
) {}
