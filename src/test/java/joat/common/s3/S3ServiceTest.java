package joat.common.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock S3Presigner presigner;

    @Test
    void presignedUrl_응답에_presignedUrl과_fileUrl이_포함된다() throws MalformedURLException {
        PresignedPutObjectRequest presignedReq = mock(PresignedPutObjectRequest.class);
        given(presignedReq.url()).willReturn(URI.create("https://s3.amazonaws.com/bucket/key?X-Amz-Signature=abc").toURL());
        given(presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedReq);

        S3Service service = new S3Service(presigner, "my-bucket", "ap-northeast-2", 300L);
        UUID userId = UUID.randomUUID();

        S3Service.PresignedUrlResponse result = service.generatePresignedUrl("photo.jpg", "image/jpeg", userId);

        assertThat(result.getPresignedUrl()).contains("X-Amz-Signature");
        assertThat(result.getFileUrl()).startsWith("https://my-bucket.s3.ap-northeast-2.amazonaws.com/");
        assertThat(result.getFileUrl()).contains(userId.toString());
        assertThat(result.getFileUrl()).endsWith("photo.jpg");
    }
}
