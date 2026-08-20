package com.college.cleanliness.controller;

import com.college.cleanliness.entity.*;
import com.college.cleanliness.security.CustomUserDetails;
import com.college.cleanliness.service.ComplaintService;
import com.college.cleanliness.service.FileService;
import com.college.cleanliness.service.NotificationService;
import com.college.cleanliness.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class ComplaintController {
    
    private final ComplaintService complaintService;
    private final UserService userService;
    private final FileService fileService;
    private final NotificationService notificationService;
    
    public ComplaintController(ComplaintService complaintService, UserService userService,
                             FileService fileService, NotificationService notificationService) {
        this.complaintService = complaintService;
        this.userService = userService;
        this.fileService = fileService;
        this.notificationService = notificationService;
    }
    
    @GetMapping("/student/complaints")
    public String studentComplaints(
            Model model, 
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<Complaint> complaints = complaintService.findByUserId(userDetails.getId(), page, size);
        
        model.addAttribute("complaints", complaints.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", complaints.getTotalPages());
        model.addAttribute("user", userDetails.getUser());
        
        return "student/complaints";
    }
    
    @GetMapping("/student/complaint/new")
    public String newComplaintForm(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("complaint", new Complaint());
        model.addAttribute("user", userDetails.getUser());
        return "student/complaint-form";
    }
    
    @PostMapping("/student/complaint/save")
    public String saveComplaint(
            @ModelAttribute Complaint complaint,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) throws IOException {
        
        if (!image.isEmpty()) {
            String imagePath = fileService.saveImage(image);
            complaint.setImagePath(imagePath);
            complaint.setOriginalFilename(image.getOriginalFilename());
        }
        
        Complaint saved = complaintService.createComplaint(complaint, userDetails.getId());
        
        if (saved != null) {
            model.addAttribute("message", "Complaint submitted successfully!");
        } else {
            model.addAttribute("error", "Failed to submit complaint");
        }
        
        return "redirect:/student/complaints";
    }
    
    @GetMapping("/student/complaint/{id}")
    public String viewComplaint(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Complaint complaint = complaintService.findById(id);
        
        if (complaint == null || !complaint.getUser().getId().equals(userDetails.getId())) {
            return "redirect:/student/complaints";
        }
        
        model.addAttribute("complaint", complaint);
        model.addAttribute("user", userDetails.getUser());
        
        return "student/complaint-view";
    }
    
    @PostMapping("/student/complaint/{id}/feedback")
    public String addFeedback(
            @PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Complaint complaint = complaintService.findById(id);
        
        if (complaint != null && complaint.getUser().getId().equals(userDetails.getId())) {
            complaintService.addFeedback(complaint.getId(), rating, comment);
        }
        
        return "redirect:/student/complaint/" + id;
    }
    
    @PostMapping("/student/complaint/{id}/verify")
    public String verifyCleanedComplaint(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Complaint complaint = complaintService.findById(id);
        
        if (complaint != null && complaint.getUser().getId().equals(userDetails.getId())) {
            complaintService.updateStatus(id, ComplaintStatus.COMPLETED, null);
        }
        
        return "redirect:/student/complaint/" + id;
    }
    
    @GetMapping("/staff/complaints")
    public String staffComplaints(
            Model model, 
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        Page<Complaint> complaints;
        if (status != null && !status.isEmpty()) {
            ComplaintStatus complaintStatus = ComplaintStatus.valueOf(status);
            List<Complaint> all = complaintService.findByStatus(complaintStatus);
            complaints = (Page<Complaint>) (Page<?>) all.subList(
                Math.min(page * size, all.size()),
                Math.min((page + 1) * size, all.size())
            );
        } else {
            complaints = complaintService.findByStaffId(userDetails.getId(), page, size);
        }
        
        List<Complaint> pendingComplaints = complaintService.findPendingComplaints();
        
        model.addAttribute("complaints", complaints.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", complaints.getTotalPages());
        model.addAttribute("pendingComplaints", pendingComplaints);
        model.addAttribute("user", userDetails.getUser());
        
        return "staff/complaints";
    }
    
    @GetMapping("/staff/complaint/{id}")
    public String viewStaffComplaint(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Complaint complaint = complaintService.findById(id);
        model.addAttribute("complaint", complaint);
        model.addAttribute("user", userDetails.getUser());
        
        return "staff/complaint-view";
    }
    
    @PostMapping("/staff/complaint/{id}/status")
    public String updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam ComplaintStatus status,
            @RequestParam(required = false) String notes,
            Model model) {
        
        try {
            complaintService.updateStatus(id, status, notes);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        
        return "redirect:/staff/complaint/" + id;
    }
    
    @PostMapping("/staff/complaint/{id}/after-image")
    public String uploadAfterImage(
            @PathVariable Long id,
            @RequestParam("afterImage") MultipartFile afterImage,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        
        if (!afterImage.isEmpty()) {
            String imagePath = fileService.saveImage(afterImage);
            complaintService.updateAfterImage(id, imagePath);
        }
        
        return "redirect:/staff/complaint/" + id;
    }
    
    @GetMapping("/notifications")
    public String notifications(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<Notification> notifications = notificationService.findByUserId(userDetails.getId(), page, size);
        Long unreadCount = notificationService.countUnread(userDetails.getId());
        
        model.addAttribute("notifications", notifications.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("unreadCount", unreadCount);
        
        return "notifications";
    }
    
    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }
}
