package com.luisa.wechat.chat.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChatUploadCleanupTask {

    private final Path chatUploadDir;

    public ChatUploadCleanupTask(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.chatUploadDir = Path.of(uploadDir).resolve("chat");
    }

    @Scheduled(cron = "${app.storage.chat-cleanup-cron:0 0 * * * *}")
    public void deleteExpiredChatFiles() {
        if (!Files.isDirectory(chatUploadDir)) {
            return;
        }
        Instant threshold = Instant.now().minus(Duration.ofHours(24));
        try (var paths = Files.list(chatUploadDir)) {
            paths.filter(Files::isRegularFile).forEach(path -> deleteIfExpired(path, threshold));
        } catch (IOException ignored) {
        }
    }

    private void deleteIfExpired(Path path, Instant threshold) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
            if (lastModifiedTime.toInstant().isBefore(threshold)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
        }
    }
}
