package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;

    public Object getStudyList() {
        return new Object();
    }

    public Object getStudy() {
        return new Object();
    }

    public Object createStudy() {
        return new Object();
    }
}
