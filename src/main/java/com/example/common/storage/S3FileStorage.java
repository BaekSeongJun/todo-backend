package com.example.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * S3 구현체. app.storage-type=s3일 때 활성화되며 PutObject로 업로드하고 S3Presigner로
 * 단기 만료 GetObject presigned URL을 발급한다(FR-F03, FR-F05). presigned URL은 클라이언트가
 * S3에 직접 접근하므로 /api/attachments/{id}/download는 이 구현체에서 호출되지 않는다
 * (PRD 8장 — app.storage-type=s3일 때 /download 사용 안 함).
 *
 * <p>업로드는 RequestBody.fromInputStream이 아니라 fromBytes를 쓴다. fromInputStream은
 * 128KiB를 초과하는 콘텐츠에 대해 재시도 시 reset()이 실패하는 SDK 차원의 제약이 있어,
 * 10MB까지 허용하는 첨부파일 업로드에서는 네트워크 재시도 상황에 조용히 실패할 수 있다.
 */
@Component
@ConditionalOnProperty(name = "app.storage-type", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3FileStorage(
            S3Client s3Client, S3Presigner s3Presigner, @Value("${app.s3-bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    @Override
    public String store(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String storedKey = UUID.randomUUID() + extension;

        try {
            PutObjectRequest request =
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storedKey)
                            .contentType(file.getContentType())
                            .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException("파일을 저장할 수 없습니다.", e);
        }

        return storedKey;
    }

    @Override
    public String generateDownloadUrl(Long attachmentId, String storedKey, long ttlSeconds) {
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().bucket(bucket).key(storedKey).build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(ttlSeconds))
                        .getObjectRequest(getObjectRequest)
                        .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public Resource load(String storedKey) {
        // s3 모드에서는 generateDownloadUrl()이 presigned URL을 직접 반환하므로 클라이언트가
        // S3에 바로 접근하고 이 메서드(/api/attachments/{id}/download 경유)는 정상 흐름에서
        // 호출되지 않는다. 조용히 값을 반환하는 대신 명시적으로 실패시켜 설계 위반을 드러낸다.
        throw new UnsupportedOperationException(
                "S3 저장소에서는 load()가 호출되지 않습니다. generateDownloadUrl()의 presigned URL로"
                        + " 직접 접근해야 합니다.");
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex == -1 ? "" : originalFilename.substring(dotIndex);
    }
}
