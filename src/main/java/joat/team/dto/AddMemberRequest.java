package joat.team.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 팀원 추가 요청 DTO.
 *
 * @param userId 추가할 멤버의 userId
 */
@Getter
@NoArgsConstructor
public class AddMemberRequest {

    @NotNull(message = "userId는 필수입니다.")
    private UUID userId;
}
