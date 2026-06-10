package joat.auth.client;

import joat.auth.dto.GoogleUserInfo;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GoogleOAuthClient {

    private final WebClient webClient;
    private final String userInfoUrl;

    public GoogleOAuthClient(@Value("${google.user-info-url}") String userInfoUrl) {
        this.webClient = WebClient.create();
        this.userInfoUrl = userInfoUrl;
    }

    public GoogleUserInfo getUserInfo(String accessToken) {
        try {
            GoogleUserInfo info = webClient.get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GoogleUserInfo.class)
                .block();
            if (info == null) throw new BusinessException(ErrorCode.OAUTH_FAILED);
            return info;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }
}
