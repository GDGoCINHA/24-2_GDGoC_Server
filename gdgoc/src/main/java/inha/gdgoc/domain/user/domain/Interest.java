package inha.gdgoc.domain.user.domain;

import lombok.Getter;

@Getter
enum Interest {
    FRONTEND("Frontend"),
    BACKEND("Backend"),
    UI_UX("UI/UX"),
    AI("AI"),
    MOBILE("Mobile"),
    GAME("Game"),
    DESIGN_3D("3D 디자인"),
    IT_PLANNING("IT 기획 및 경영"),
    PM("PM"),
    STARTUP("스타트업"),
    ETC("기타");

    private final String area;

    Interest(String area) {
        this.area = area;
    }

}
