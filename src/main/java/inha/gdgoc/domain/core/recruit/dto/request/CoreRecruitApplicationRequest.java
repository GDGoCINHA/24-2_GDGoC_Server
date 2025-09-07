package inha.gdgoc.domain.core.recruit.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CoreRecruitApplicationRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String studentId;

    @NotBlank
    private String phone;

    @NotBlank
    private String major;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String team;

    @NotBlank
    private String motivation;

    @NotBlank
    private String wish;

    @NotBlank
    private String strengths;

    @NotBlank
    private String pledge;

    @NotNull
    private List<String> fileUrls;

    @Builder
    public CoreRecruitApplicationRequest(String name, String studentId, String phone, String major,
        String email, String team, String motivation, String wish, String strengths, String pledge,
        List<String> fileUrls) {
        this.name = name;
        this.studentId = studentId;
        this.phone = phone;
        this.major = major;
        this.email = email;
        this.team = team;
        this.motivation = motivation;
        this.wish = wish;
        this.strengths = strengths;
        this.pledge = pledge;
        this.fileUrls = fileUrls;
    }
}


