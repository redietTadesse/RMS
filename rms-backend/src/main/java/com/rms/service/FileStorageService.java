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

@Service
public class FileStorageService {

    private final Path rootDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * Store a receipt file
     */
    public String storeReceipt(MultipartFile file, Long invoiceId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

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

    /**
     * Store a tenant document
     */
    public String storeDocument(MultipartFile file, Long tenantId, String type) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String original = file.getOriginalFilename();
        String extension = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf("."))
                : "";

        // Store in tenant-specific folder with type prefix
        String storedName = type + "_" + System.currentTimeMillis() + extension;

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

    /**
     * Generic store method (backward compatibility)
     */
    public Path store(Long tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

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

    /**
     * Delete a file
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filePath);
            return false;
        }
    }

    /**
     * Generate a unique stored name
     */
    private String generateStoredName(MultipartFile file) {
        String original = file.getOriginalFilename();
        String safeName = original != null
                ? Paths.get(original).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
    }
}