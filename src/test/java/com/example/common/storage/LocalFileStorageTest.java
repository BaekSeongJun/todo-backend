package com.example.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {

    private static final String SECRET =
            "test-secret-key-for-local-file-storage-unit-test-must-be-long-enough";

    @TempDir Path tempDir;

    @Test
    void 파일을_저장하면_UUID_파일명으로_업로드_디렉터리에_생성된다() throws IOException {
        LocalFileStorage storage =
                new LocalFileStorage(tempDir.toString(), new AttachmentTokenProvider(SECRET));
        MockMultipartFile file =
                new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());

        String storedKey = storage.store(file);

        assertThat(storedKey).endsWith(".txt");
        assertThat(Files.exists(tempDir.resolve(storedKey))).isTrue();
        assertThat(Files.readString(tempDir.resolve(storedKey))).isEqualTo("hello");
    }

    @Test
    void 다운로드_URL은_서명_토큰이_포함된_download_엔드포인트_경로를_반환한다() {
        LocalFileStorage storage =
                new LocalFileStorage(tempDir.toString(), new AttachmentTokenProvider(SECRET));

        String url = storage.generateDownloadUrl(42L, "any-key.txt", 300);

        assertThat(url).startsWith("/api/attachments/42/download?token=");
    }

    @Test
    void 저장된_파일을_로드할_수_있다() throws IOException {
        LocalFileStorage storage =
                new LocalFileStorage(tempDir.toString(), new AttachmentTokenProvider(SECRET));
        MockMultipartFile file =
                new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());
        String storedKey = storage.store(file);

        Resource resource = storage.load(storedKey);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsByteArray()).isEqualTo("hello".getBytes());
    }
}
