package inha.gdgoc.domain.auth.dto.request;

import inha.gdgoc.global.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindIdRequest extends BaseEntity {
    private String name;
    private String major;
    private String phoneNumber;
}
