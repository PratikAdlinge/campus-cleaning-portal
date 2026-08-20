package com.college.cleanliness.service;

import com.college.cleanliness.entity.Notification;
import com.college.cleanliness.entity.User;
import com.college.cleanliness.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }
    
    @Transactional
    public Notification createNotification(User user, String title, String message, Object relatedObject) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        
        if (relatedObject instanceof com.college.cleanliness.entity.Complaint) {
            notification.setComplaint((com.college.cleanliness.entity.Complaint) relatedObject);
        }
        
        return notificationRepository.save(notification);
    }
    
    public Page<Notification> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    public Long countUnread(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }
    
    @Transactional
    public void markAllAsRead(Long userId) {
        Page<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId, PageRequest.of(0, Integer.MAX_VALUE));
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }
    
    public Notification findById(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }
}
