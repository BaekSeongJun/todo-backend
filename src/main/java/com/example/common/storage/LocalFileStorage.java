package com.example.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 개발용 구현체. app.storage-type=local(기본값)일 때 활성화되며
 * APP_UPLOAD_DIR 경로에 파일을 직접 저장한다. 다운로드 URL은 저장소 원본 경로를
 * 노출하지 않고 서명 토큰이 포함된 자체 엔드포인트 URL을 반환한다(FR-F05).
 */
@Component
@ConditionalOnProperty(name = "app.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path uploadDir;
    private final AttachmentTokenProvider tokenProvider;

    public LocalFileStorage(
            @Value("${app.upload-dir}") String uploadDir, AttachmentTokenProvider tokenProvider) {
        this.uploadDir = Path.of(uploadDir);
        this.tokenProvider = tokenProvider;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 디렉터리를 생성할 수 없습니다: " + uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String storedKey = UUID.randomUUID() + extension;

        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(storedKey));
        } catch (IOException e) {
            throw new UncheckedIOException("파일을 저장할 수 없습니다.", e);
        }

        return storedKey;
    }

    @Override
    public String generateDownloadUrl(Long attachmentId, String storedKey, long ttlSeconds) {
        String token = tokenProvider.generateToken(attachmentId, ttlSeconds);
        return "/api/attachments/" + attachmentId + "/download?token=" + token;
    }

    @Override
    public Resource load(String storedKey) {
        try {
            Resource resource = new UrlResource(uploadDir.resolve(storedKey).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new UncheckedIOException(new IOException("파일을 읽을 수 없습니다: " + storedKey));
            }
            return resource;
        } catch (java.net.MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex == -1 ? "" : originalFilename.substring(dotIndex);
    }
}
