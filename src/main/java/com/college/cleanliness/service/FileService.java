package com.college.cleanliness.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {
    
    @Value("${app.image.upload-dir}")
    private String uploadDir;
    
    public String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        Path uploadPath = getAbsoluteUploadPath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);
        
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        if (originalImage != null) {
            BufferedImage compressedImage = compressImage(originalImage, 800, 600);
            String format = extension.replace(".", "").toLowerCase();
            if (format.isEmpty()) format = "jpg";
            ImageIO.write(compressedImage, format, filePath.toFile());
        } else {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        return "/uploads/" + filename;
    }
    
    private Path getAbsoluteUploadPath() {
        Path basePath = Paths.get("").toAbsolutePath();
        return basePath.resolve(uploadDir);
    }
    
    private BufferedImage compressImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        double scale = Math.min(
            (double) maxWidth / width,
            (double) maxHeight / height
        );
        
        if (scale >= 1) {
            return originalImage;
        }
        
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        
        return resizedImage;
    }
    
    public boolean deleteImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }
        
        try {
            String filename = imagePath.substring(imagePath.lastIndexOf("/") + 1);
            Path filePath = getAbsoluteUploadPath().resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
    
    public File getImageFile(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        String filename = imagePath.substring(imagePath.lastIndexOf("/") + 1);
        Path filePath = getAbsoluteUploadPath().resolve(filename);
        
        if (Files.exists(filePath)) {
            return filePath.toFile();
        }
        return null;
    }
}
