package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.repository.StudyAttendeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyAttendeeService {

    private final StudyAttendeeRepository studyAttendeeRepository;

    public Object getAttendeeList() {
        return new Object();
    }

    public Object createAttendee() {
        return new Object();
    }

    public Object updateAttendee() {
        return new Object();
    }
}
