package inha.gdgoc.domain.auth.dto.response;

public record LoginResponse(boolean exists, String access_token){
}
