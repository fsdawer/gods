package joat.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * WebSocket 채팅 메시지 전송 요청 DTO.
 * /app/team/{teamId}/message 로 전송된다.
 * senderId는 서버에서 Principal로 결정하므로 클라이언트가 전달하지 않는다.
 *
 * @param content 메시지 본문
 */
@Getter
@NoArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;
}
