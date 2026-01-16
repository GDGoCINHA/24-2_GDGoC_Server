package inha.gdgoc.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoogleUserInfo {
    private String sub;
    private String email;
    private String name;
}