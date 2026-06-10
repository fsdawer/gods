package joat.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 팀방 생성 요청 DTO.
 *
 * @param name      팀방 이름 (필수, 최대 100자)
 * @param memberIds 초대할 멤버 userId 목록 (최소 1명)
 */
@Getter
@NoArgsConstructor
public class CreateTeamRequest {

    @NotBlank(message = "팀방 이름은 필수입니다.")
    @Size(max = 100, message = "팀방 이름은 최대 100자입니다.")
    private String name;

    @NotEmpty(message = "팀원을 최소 1명 선택해야 합니다.")
    private List<UUID> memberIds;
}
