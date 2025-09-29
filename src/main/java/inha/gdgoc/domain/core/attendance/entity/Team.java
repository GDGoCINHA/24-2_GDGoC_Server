// inha/gdgoc/domain/core/attendance/entity/Team.java
package inha.gdgoc.domain.core.attendance.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Team {
    private String id;
    @Setter
    private String name;
    private String lead;
    private final List<Member> members = new ArrayList<>();

    public Team(String id, String name, String lead){
        this.id = id; this.name = name; this.lead = lead == null ? "" : lead;
    }

    public void setLead(String lead){ this.lead = lead == null ? "" : lead; }
}