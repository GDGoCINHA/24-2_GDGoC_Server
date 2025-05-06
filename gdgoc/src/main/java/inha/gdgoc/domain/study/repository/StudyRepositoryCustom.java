package inha.gdgoc.domain.study.repository;

import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.enums.CreatorType;
import inha.gdgoc.domain.study.enums.StudyStatus;

import java.util.List;
import java.util.Optional;

public interface StudyRepositoryCustom {

    Optional<Study> findOneWithUserById(Long id);

    List<Study> findAllByUserId(Long userId);

    List<Study> findAllByStatusAndCreatorType(
            Optional<StudyStatus> status,
            Optional<CreatorType> creatorType,
            Long limit,
            Long offset
    );

    Long findAllCountByStatusAndCreatorType(
            Optional<StudyStatus> status,
            Optional<CreatorType> creatorType
    );
}
