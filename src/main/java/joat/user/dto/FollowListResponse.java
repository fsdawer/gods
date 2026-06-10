package joat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 팔로워/팔로잉 목록 응답 DTO.
 * GET /api/users/{userId}/followers, GET /api/users/{userId}/following 응답에 사용된다.
 * 각 항목에 닉네임, 프로필 이미지, 팔로우 여부를 포함하여 앱이 추가 API 호출 없이 렌더링할 수 있다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FollowListResponse {
    /** 팔로워 또는 팔로잉 유저 요약 목록 */
    private List<UserSummary> users;
}
