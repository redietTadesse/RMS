package com.rms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    
    public String storeReceipt(MultipartFile file, Long invoiceId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String original = file.getOriginalFilename();
        String safeName = original != null
                ? Paths.get(original).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "receipt";

        String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;

        try {
            Path receiptDir = rootDir.resolve("receipts");
            Files.createDirectories(receiptDir);
            Path target = receiptDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store receipt: " + safeName, e);
        }
    }

    
    public Path store(Long tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String original = file.getOriginalFilename();
        String safeName = original != null
                ? Paths.get(original).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";

        String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;

        try {
            Path tenantDir = rootDir.resolve("tenant-" + tenantId);
            Files.createDirectories(tenantDir);
            Path target = tenantDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + safeName, e);
        }
    }
}