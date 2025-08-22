package org.acme.Storage;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@ApplicationScoped
public class FileStorageService {
    @ConfigProperty(name = "upload.directory")
    String uploadDir;

    public String store(FileUpload fileUpload) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = fileUpload.fileName();
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        String newFileName = UUID.randomUUID().toString() + extension;

        Path destination = uploadPath.resolve(newFileName);
        Files.copy(fileUpload.uploadedFile(), destination, StandardCopyOption.REPLACE_EXISTING);

        return newFileName;
    }

    public boolean delete(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Error deleting file: " + fileName + " - " + e.getMessage());
            return false;
        }
    }
}
