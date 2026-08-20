package com.college.cleanliness.service;

import com.college.cleanliness.entity.*;
import com.college.cleanliness.repository.ComplaintRepository;
import com.college.cleanliness.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ComplaintService {
    
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    
    public ComplaintService(ComplaintRepository complaintRepository, UserRepository userRepository, 
                          NotificationService notificationService, UserService userService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.userService = userService;
    }
    
    @Transactional
    public Complaint createComplaint(Complaint complaint, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            complaint.setUser(user);
            complaint.setStatus(ComplaintStatus.PENDING);
            
            if (complaint.getPriority() == null) {
                complaint.setPriority(Priority.MEDIUM);
            }
            
            Complaint savedComplaint = complaintRepository.save(complaint);
            
            notifyStaff(savedComplaint);
            
            return savedComplaint;
        }
        return null;
    }
    
    private void notifyStaff(Complaint complaint) {
        List<User> staffList = userRepository.findAllStaff();
        for (User staff : staffList) {
            String message = "New " + complaint.getPriority() + " priority complaint: " + complaint.getTitle();
            notificationService.createNotification(staff, "New Complaint", message, complaint);
        }
    }
    
    public Complaint findById(Long id) {
        return complaintRepository.findById(id).orElse(null);
    }
    
    public Page<Complaint> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return complaintRepository.findByUserId(userId, pageable);
    }
    
    public Page<Complaint> findByStaffId(Long staffId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return complaintRepository.findByStaffId(staffId, pageable);
    }
    
    public List<Complaint> findByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatus(status);
    }
    
    public List<Complaint> findActiveComplaints() {
        return complaintRepository.findActiveComplaints();
    }
    
    public List<Complaint> findPendingComplaints() {
        return complaintRepository.findPendingComplaintsSorted();
    }
    
    @Transactional
    public Complaint assignToStaff(Long complaintId, Long staffId, Long assignedById) {
        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        User staff = userRepository.findById(staffId).orElse(null);
        User assignedBy = userRepository.findById(assignedById).orElse(null);
        
        if (complaint != null && staff != null) {
            complaint.setStaff(staff);
            complaint.setAssignedBy(assignedBy);
            complaint.setStatus(ComplaintStatus.ASSIGNED);
            complaint.setAssignedAt(LocalDateTime.now());
            
            Complaint saved = complaintRepository.save(complaint);
            
            String message = "Complaint #" + complaintId + " has been assigned to you";
            notificationService.createNotification(staff, "Complaint Assigned", message, complaint);
            
            return saved;
        }
        return null;
    }
    
    @Transactional
    public Complaint autoAssign(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null) return null;
        
        List<User> staffList = userRepository.findAllStaff();
        if (staffList.isEmpty()) return null;
        
        User selectedStaff = staffList.stream()
            .min((a, b) -> {
                long countA = complaintRepository.findByStatusAndStaffId(ComplaintStatus.ASSIGNED, a.getId()).size();
                long countB = complaintRepository.findByStatusAndStaffId(ComplaintStatus.ASSIGNED, b.getId()).size();
                return Long.compare(countA, countB);
            })
            .orElse(null);
        
        if (selectedStaff != null) {
            return assignToStaff(complaintId, selectedStaff.getId(), null);
        }
        return null;
    }
    
    @Transactional
    public Complaint updateStatus(Long complaintId, ComplaintStatus status, String notes) {
        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint != null) {
            if ((status == ComplaintStatus.CLEANED || status == ComplaintStatus.COMPLETED) 
                && (complaint.getAfterImagePath() == null || complaint.getAfterImagePath().isEmpty())) {
                throw new IllegalStateException("Cannot mark as " + status + " without uploading after cleaning photo. Please upload the after cleaning image first.");
            }
            
            complaint.setStatus(status);
            
            if (status == ComplaintStatus.IN_PROGRESS) {
                complaint.setAssignedAt(LocalDateTime.now());
            } else if (status == ComplaintStatus.CLEANED) {
                complaint.setCompletedAt(LocalDateTime.now());
            } else if (status == ComplaintStatus.COMPLETED) {
                complaint.setCompletedAt(LocalDateTime.now());
                complaint.setResolutionNotes(notes);
                
                if (complaint.getStaff() != null) {
                    userService.updatePoints(complaint.getStaff(), 10);
                }
            } else if (status == ComplaintStatus.VERIFIED) {
                complaint.setVerifiedAt(LocalDateTime.now());
                
                if (complaint.getUser() != null) {
                    userService.updatePoints(complaint.getUser(), 5);
                }
                if (complaint.getStaff() != null) {
                    userService.updatePoints(complaint.getStaff(), 10);
                }
            } else if (status == ComplaintStatus.REOPENED) {
                complaint.setCompletedAt(null);
                complaint.setVerifiedAt(null);
            }
            
            return complaintRepository.save(complaint);
        }
        return null;
    }
    
    @Transactional
    public Complaint updateAfterImage(Long complaintId, String afterImagePath) {
        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint != null) {
            complaint.setAfterImagePath(afterImagePath);
            complaint.setStatus(ComplaintStatus.CLEANED);
            complaint.setCompletedAt(LocalDateTime.now());
            
            Complaint saved = complaintRepository.save(complaint);
            
            if (complaint.getUser() != null) {
                String message = "Your complaint #" + complaintId + " has been cleaned! Please verify.";
                notificationService.createNotification(complaint.getUser(), "Complaint Cleaned", message, complaint);
            }
            
            return saved;
        }
        return null;
    }
    
    @Transactional
    public Complaint addFeedback(Long complaintId, Integer rating, String comment) {
        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint != null) {
            complaint.setRating(rating);
            complaint.setFeedbackComment(comment);
            return complaintRepository.save(complaint);
        }
        return null;
    }
    
    public Long countByStatus(ComplaintStatus status) {
        if (status == null) {
            return complaintRepository.count();
        }
        return complaintRepository.countByStatus(status);
    }
    
    public Long countByUserId(Long userId) {
        return complaintRepository.countByUserId(userId);
    }
    
    public Long countCompletedByStaffId(Long staffId) {
        return complaintRepository.countCompletedByStaffId(staffId);
    }
    
    public Double getAverageResolutionTime() {
        return complaintRepository.getAverageResolutionTimeMinutes();
    }
    
    public Map<ComplaintLocation, Long> countByLocationType() {
        List<Object[]> results = complaintRepository.countByLocationType();
        return results.stream()
            .collect(Collectors.toMap(
                r -> (ComplaintLocation) r[0],
                r -> (Long) r[1]
            ));
    }
    
    public Page<Complaint> findAll(int page, int size, String status, String priority) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return complaintRepository.findAll(pageable);
    }
    
    public List<Complaint> findAll() {
        return complaintRepository.findAll();
    }
    
    public void deleteById(Long id) {
        complaintRepository.deleteById(id);
    }
}
