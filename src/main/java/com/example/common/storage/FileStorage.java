package com.example.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화(FR-F03). 로컬 개발은 디스크에, AWS는 S3에 저장하며
 * APP_STORAGE_TYPE 환경변수로 구현체를 전환한다. 코드는 저장소 종류를 알 필요가 없다.
 */
public interface FileStorage {

    /** UUID 기반 저장 키를 생성해 파일을 저장하고 그 키를 반환한다(FR-F04). */
    String store(MultipartFile file);

    /**
     * 단기 만료 다운로드 URL을 발급한다(FR-F05). S3 구현체는 presigned URL을,
     * 로컬 구현체는 서명된 만료 토큰이 포함된 자체 다운로드 엔드포인트 URL을 반환한다.
     * attachmentId는 로컬 구현체가 {@code /api/attachments/{id}/download} 경로를
     * 조립하는 데 필요하다.
     */
    String generateDownloadUrl(Long attachmentId, String storedKey, long ttlSeconds);

    /** {@code /download} 스트리밍을 위해 저장된 파일을 읽어온다. */
    Resource load(String storedKey);
}
