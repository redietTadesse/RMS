package com.rms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;

@Service
public class FileStorageService {

    private final Path rootDir;

    @Value("${app.upload.max-size:10485760}")
    private long maxFileSize;

    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    };
        private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".doc", ".docx"
        );

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String storeReceipt(MultipartFile file, Long invoiceId) {
        validateFile(file);
        String storedName = generateStoredName(file);

        try {
            Path receiptDir = rootDir.resolve("receipts");
            Files.createDirectories(receiptDir);
            Path target = receiptDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store receipt: " + storedName, e);
        }
    }

    public String storeDocument(MultipartFile file, Long tenantId, String type) {
        validateFile(file);

        String original = file.getOriginalFilename();
        String safeOriginal = original == null ? "" : Paths.get(original).getFileName().toString();
        String extension = safeOriginal.contains(".")
            ? safeOriginal.substring(safeOriginal.lastIndexOf('.')).toLowerCase(Locale.ROOT)
            : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File extension not allowed: " + extension);
        }
        String safeType = type == null ? "document" : type.replaceAll("[^a-zA-Z0-9_-]", "_");
        String storedName = safeType + "_" + System.currentTimeMillis() + "_"
            + UUID.randomUUID().toString().substring(0, 8) + extension;

        try {
            Path tenantDir = rootDir.resolve("tenants").resolve("tenant-" + tenantId).resolve("documents");
            Files.createDirectories(tenantDir);
            Path target = tenantDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store document: " + storedName, e);
        }
    }

    public Path store(Long tenantId, MultipartFile file) {
        validateFile(file);
        String storedName = generateStoredName(file);

        try {
            Path tenantDir = rootDir.resolve("tenant-" + tenantId);
            Files.createDirectories(tenantDir);
            Path target = tenantDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storedName, e);
        }
    }

    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds limit: " + (maxFileSize / 1048576) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Could not determine file type");
        }

        boolean allowed = false;
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }
    }

    private String generateStoredName(MultipartFile file) {
        String original = file.getOriginalFilename();
        String safeName = original != null
                ? Paths.get(original).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
    }
}