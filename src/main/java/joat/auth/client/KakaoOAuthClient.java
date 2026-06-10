package joat.auth.client;

import joat.auth.dto.KakaoUserInfo;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class KakaoOAuthClient {

    private final WebClient webClient;
    private final String userInfoUrl;

    public KakaoOAuthClient(@Value("${kakao.user-info-url}") String userInfoUrl) {
        this.webClient = WebClient.create();
        this.userInfoUrl = userInfoUrl;
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            KakaoUserInfo info = webClient.get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserInfo.class)
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
