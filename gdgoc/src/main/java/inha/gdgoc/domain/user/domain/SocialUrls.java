package inha.gdgoc.domain.user.domain;

import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
class SocialUrls implements Serializable {
    private SnsType snsType;
    private String url;

    public void validateUrl() {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        String urlPattern = "^https?://.*$";
        if (!url.matches(urlPattern)) {
            throw new IllegalArgumentException("Invalid URL format");
        }
    }
}
