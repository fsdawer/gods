package joat.user.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.user.entity.OAuthProvider;
import joat.user.entity.User;
import joat.user.repository.FollowRepository;
import joat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks UserServiceImpl userService;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;

    @Test
    void 자기자신을_팔로우하면_예외가_발생한다() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> userService.follow(id, id))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.CANNOT_FOLLOW_SELF);
    }

    @Test
    void 이미_팔로우_중이면_ALREADY_FOLLOWING_예외가_발생한다() {
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        given(userRepository.findById(target))
            .willReturn(Optional.of(User.of("target", OAuthProvider.kakao, "id")));
        given(followRepository.existsByFollowerIdAndFollowingId(me, target)).willReturn(true);

        assertThatThrownBy(() -> userService.follow(me, target))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.ALREADY_FOLLOWING);
    }

    @Test
    void 팔로우_안한_상태에서_언팔로우하면_예외가_발생한다() {
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        given(followRepository.existsByFollowerIdAndFollowingId(me, target)).willReturn(false);

        assertThatThrownBy(() -> userService.unfollow(me, target))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOLLOWING);
    }
}
