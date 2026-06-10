package joat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 수정 요청 DTO.
 * PATCH /api/users/me 에서 사용된다.
 * 변경할 필드만 포함하고, null인 필드는 기존값을 유지한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    /** 변경할 닉네임 (null이면 기존값 유지) */
    private String nickname;
    /** 변경할 프로필 이미지 URL (null이면 기존값 유지) */
    private String profileImageUrl;
    /** 변경할 자기소개 (null이면 기존값 유지) */
    private String bio;
}
