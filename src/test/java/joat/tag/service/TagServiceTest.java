package joat.tag.service;

import joat.tag.entity.Tag;
import joat.tag.dto.TagResponse;
import joat.tag.repository.PostTagRepository;
import joat.tag.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @InjectMocks TagServiceImpl tagService;
    @Mock TagRepository tagRepository;
    @Mock PostTagRepository postTagRepository;
    @Mock RedisTemplate<String, Object> redisTemplate;

    @Test
    void Redis에_트렌딩_데이터_있으면_DB_쿼리_없이_반환한다() {
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);
        given(zSetOps.reverseRange("tags:trending", 0, 19)).willReturn(Set.of("갓생", "공부"));
        given(tagRepository.findByName("갓생")).willReturn(java.util.Optional.of(Tag.of("갓생")));
        given(tagRepository.findByName("공부")).willReturn(java.util.Optional.of(Tag.of("공부")));

        List<TagResponse> result = tagService.getTrending();

        assertThat(result).hasSize(2);
    }

    @Test
    void Redis_비어있으면_DB에서_트렌딩_조회한다() {
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);
        given(zSetOps.reverseRange("tags:trending", 0, 19)).willReturn(Set.of());
        given(tagRepository.findTop20ByOrderByPostCountDesc())
            .willReturn(List.of(Tag.of("갓생"), Tag.of("개발")));

        List<TagResponse> result = tagService.getTrending();

        assertThat(result).hasSize(2);
    }
}
