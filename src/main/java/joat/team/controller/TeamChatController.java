package joat.team.controller;

import joat.team.dto.SendMessageRequest;
import joat.team.dto.TeamMessageResponse;
import joat.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP WebSocket 채팅 메시지 핸들러.
 * 클라이언트는 /app/team/{teamId}/message 로 메시지를 전송하면
 * /topic/team/{teamId} 구독자 전원에게 브로드캐스트된다.
 */
@Controller
@RequiredArgsConstructor
public class TeamChatController {

    private final TeamService teamService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 수신 및 브로드캐스트.
     * senderId는 WebSocket 핸드셰이크 시 인증된 Principal에서 추출한다.
     * 메시지를 DB에 저장한 뒤 해당 팀방 구독자 전원에게 브로드캐스트된다.
     *
     * @param teamId    채팅 대상 팀방 ID (경로 변수)
     * @param request   전송 요청 (content만 포함)
     * @param principal WebSocket 세션의 인증 주체
     */
    @MessageMapping("/team/{teamId}/message")
    public void sendMessage(
            @DestinationVariable UUID teamId,
            SendMessageRequest request,
            Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        TeamMessageResponse response = teamService.saveMessage(teamId, senderId, request.getContent());
        messagingTemplate.convertAndSend("/topic/team/" + teamId, response);
    }
}
