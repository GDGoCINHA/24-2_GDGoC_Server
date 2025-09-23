// inha/gdgoc/domain/core/attendance/entity/Member.java
package inha.gdgoc.domain.core.attendance.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Member {
    private final String id;
    @Setter
    private String name;

    public Member(String id, String name){ this.id = id; this.name = name; }
}