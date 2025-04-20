package inha.gdgoc.domain.study.repository;

import inha.gdgoc.domain.study.entity.Study;

import java.util.Optional;

public interface StudyRepositoryCustom {

    Optional<Study> findOneWithUserById(Long id);
}
