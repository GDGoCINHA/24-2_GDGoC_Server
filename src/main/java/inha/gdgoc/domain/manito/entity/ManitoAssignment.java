package inha.gdgoc.domain.manito.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "manito_assignments", uniqueConstraints = {@UniqueConstraint(name = "uq_manito_assignment_per_student", columnNames = {"session_id", "student_id"})}, indexes = {@Index(name = "idx_manito_assignments_session", columnList = "session_id"), @Index(name = "idx_manito_assignments_student", columnList = "student_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ManitoAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ManitoSession session;

    @Column(name = "student_id", nullable = false, length = 32)
    private String studentId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    /**
     * clientKey/hash로만 복호화 가능한 암호문
     * 처음 업로드 시에는 아직 없을 수 있으므로 nullable 허용
     */
    @Lob
    @Column(name = "encrypted_manitto") // nullable = true (default)
    private String encryptedManitto;

    @Column(name = "pin_hash", nullable = false, length = 255)
    private String pinHash;

    @Builder
    private ManitoAssignment(ManitoSession session, String studentId, String name, String encryptedManitto, String pinHash) {
        this.session = session;
        this.studentId = studentId;
        this.name = name;
        this.encryptedManitto = encryptedManitto;
        this.pinHash = pinHash;
    }

    public void changePinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public void changeEncryptedManitto(String encryptedManitto) {
        this.encryptedManitto = encryptedManitto;
    }

    public void changeName(String name) {
        this.name = name;
    }
}