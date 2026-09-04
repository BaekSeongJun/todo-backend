package com.example.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3FileStorageTest {

    private final S3Client s3Client = Mockito.mock(S3Client.class);
    private final S3Presigner s3Presigner = Mockito.mock(S3Presigner.class);
    private final S3FileStorage storage = new S3FileStorage(s3Client, s3Presigner, "test-bucket");

    @Test
    void 파일을_저장하면_UUID_키로_PutObject를_호출하고_그_키를_반환한다() {
        Mockito.when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile file =
                new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());

        String storedKey = storage.store(file);

        assertThat(storedKey).endsWith(".txt");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        Mockito.verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo(storedKey);
        assertThat(captor.getValue().contentType()).isEqualTo("text/plain");
    }

    @Test
    void 다운로드_URL은_S3Presigner가_만든_presigned_URL_문자열을_반환한다() throws MalformedURLException {
        URL presignedUrl =
                new URL("https://test-bucket.s3.ap-northeast-2.amazonaws.com/key.txt?X-Amz-Signature=abc");
        PresignedGetObjectRequest presignedRequest = Mockito.mock(PresignedGetObjectRequest.class);
        Mockito.when(presignedRequest.url()).thenReturn(presignedUrl);
        Mockito.when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        String url = storage.generateDownloadUrl(42L, "key.txt", 300);

        assertThat(url).isEqualTo(presignedUrl.toString());
        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        Mockito.verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("key.txt");
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    void load는_UnsupportedOperationException을_던진다() {
        assertThatThrownBy(() -> storage.load("any-key.txt"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
