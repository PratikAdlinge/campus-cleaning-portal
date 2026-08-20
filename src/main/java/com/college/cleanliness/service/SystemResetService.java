package com.college.cleanliness.service;

import com.college.cleanliness.repository.ComplaintRepository;
import com.college.cleanliness.repository.NotificationRepository;
import com.college.cleanliness.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

@Service
public class SystemResetService {
    
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DataSource dataSource;
    
    public SystemResetService(ComplaintRepository complaintRepository, 
                              UserRepository userRepository,
                              NotificationRepository notificationRepository,
                              DataSource dataSource) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.dataSource = dataSource;
    }
    
    @Transactional
    public void resetSystem() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            stmt.execute("TRUNCATE TABLE notifications");
            stmt.execute("TRUNCATE TABLE complaints");
            stmt.execute("DELETE FROM user_roles WHERE user_id NOT IN (SELECT id FROM users WHERE email = 'admin@college.com')");
            stmt.execute("DELETE FROM users WHERE email != 'admin@college.com'");
            
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to reset system: " + e.getMessage());
        }
        
        deleteAllUploadedImages();
    }
    
    private void deleteAllUploadedImages() {
        try {
            Path uploadPath = Paths.get("uploads");
            if (Files.exists(uploadPath)) {
                File[] files = uploadPath.toFile().listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            Files.delete(file.toPath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting uploaded files: " + e.getMessage());
        }
    }
    
    public long getTotalComplaints() {
        return complaintRepository.count();
    }
    
    public long getTotalUsers() {
        return userRepository.count();
    }
}
