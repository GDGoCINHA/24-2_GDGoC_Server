package inha.gdgoc.domain.core.recruit.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "core_recruit_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoreRecruitApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "major", nullable = false)
    private String major;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "team", nullable = false)
    private String team;

    @Column(name = "motivation", nullable = false, columnDefinition = "text")
    private String motivation;

    @Column(name = "wish", nullable = false, columnDefinition = "text")
    private String wish;

    @Column(name = "strengths", nullable = false, columnDefinition = "text")
    private String strengths;

    @Column(name = "pledge", nullable = false, columnDefinition = "text")
    private String pledge;

    @Type(JsonType.class)
    @Column(name = "file_urls", nullable = false, columnDefinition = "jsonb")
    private List<String> fileUrls;

    public Long getId() {
        return id;
    }
}


