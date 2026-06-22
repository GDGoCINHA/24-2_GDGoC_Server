package inha.gdgoc.domain.board.notice.dto.request;

import inha.gdgoc.domain.board.notice.enums.CategoryEnum;
import inha.gdgoc.domain.board.notice.enums.SearchTypeEnum;

public record NoticeSearchCondition(
    CategoryEnum category,
    SearchTypeEnum searchType,
    String keyword
) {}
