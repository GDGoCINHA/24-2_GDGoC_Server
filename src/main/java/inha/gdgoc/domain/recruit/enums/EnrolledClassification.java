package inha.gdgoc.domain.recruit.enums;

import lombok.Getter;

@Getter
public enum EnrolledClassification {
    FULL_REGISTRATION("정등록"),
    LEAVE_OF_ABSENCE("휴학"),
    GRADUATION("졸업"),
    PARTIAL_REGISTRATION("부분등록"),
    COMPLETION("수료");

    private final String status;

    EnrolledClassification(String status) {
        this.status = status;
    }

    public static EnrolledClassification fromStatus(String status) {
        for (EnrolledClassification classification : EnrolledClassification.values()) {
            if (classification.status.equals(status)) {
                return classification;
            }
        }
        throw new IllegalArgumentException("Invalid enrolledClassification value: " + status);
    }
}
