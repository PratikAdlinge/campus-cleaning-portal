package com.college.cleanliness.controller;

import com.college.cleanliness.entity.*;
import com.college.cleanliness.repository.ComplaintRepository;
import com.college.cleanliness.security.CustomUserDetails;
import com.college.cleanliness.service.ComplaintService;
import com.college.cleanliness.service.NotificationService;
import com.college.cleanliness.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class DashboardController {
    
    private final UserService userService;
    private final ComplaintService complaintService;
    private final NotificationService notificationService;
    private final ComplaintRepository complaintRepository;
    
    public DashboardController(UserService userService, ComplaintService complaintService,
                             NotificationService notificationService, ComplaintRepository complaintRepository) {
        this.userService = userService;
        this.complaintService = complaintService;
        this.notificationService = notificationService;
        this.complaintRepository = complaintRepository;
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
        
        model.addAttribute("user", userDetails.getUser());
        
        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isStaff) {
            return "redirect:/staff/dashboard";
        } else {
            return "redirect:/student/dashboard";
        }
    }
    
    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        
        Page<Complaint> complaints = complaintService.findByUserId(user.getId(), 0, 5);
        
        Long totalComplaints = complaintService.countByUserId(user.getId());
        
        List<Notification> notifications = notificationService.findByUserId(user.getId(), 0, 5).getContent();
        Long unreadCount = notificationService.countUnread(user.getId());
        
        model.addAttribute("user", user);
        model.addAttribute("complaints", complaints.getContent());
        model.addAttribute("totalComplaints", totalComplaints);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("points", user.getPoints());
        
        return "student/dashboard";
    }
    
    @GetMapping("/staff/dashboard")
    public String staffDashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        
        List<Complaint> assignedComplaints = complaintService.findByStatus(ComplaintStatus.ASSIGNED);
        assignedComplaints = assignedComplaints.stream()
            .filter(c -> c.getStaff() != null && c.getStaff().getId().equals(user.getId()))
            .toList();
        
        List<Complaint> inProgress = complaintService.findByStatus(ComplaintStatus.IN_PROGRESS);
        inProgress = inProgress.stream()
            .filter(c -> c.getStaff() != null && c.getStaff().getId().equals(user.getId()))
            .toList();
        
        Long completedCount = complaintService.countCompletedByStaffId(user.getId());
        
        List<Notification> notifications = notificationService.findByUserId(user.getId(), 0, 5).getContent();
        Long unreadCount = notificationService.countUnread(user.getId());
        
        model.addAttribute("user", user);
        model.addAttribute("assignedComplaints", assignedComplaints);
        model.addAttribute("inProgressComplaints", inProgress);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("points", user.getPoints());
        
        return "staff/dashboard";
    }
    
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long totalComplaints = complaintRepository.count();
        Long pendingComplaints = complaintService.countByStatus(ComplaintStatus.PENDING);
        Long completedComplaints = complaintService.countByStatus(ComplaintStatus.COMPLETED);
        Long verifiedComplaints = complaintService.countByStatus(ComplaintStatus.VERIFIED);
        
        Long studentCount = userService.countStudents();
        Long staffCount = userService.countStaff();
        
        Double avgResolutionTime = complaintService.getAverageResolutionTime();
        
        List<User> topStaff = userService.findTopStaffByPoints();
        
        List<Complaint> recentComplaints = complaintService.findAll().stream()
            .limit(10)
            .toList();
        
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("totalComplaints", totalComplaints);
        model.addAttribute("pendingComplaints", pendingComplaints);
        model.addAttribute("completedComplaints", completedComplaints);
        model.addAttribute("verifiedComplaints", verifiedComplaints);
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("staffCount", staffCount);
        model.addAttribute("avgResolutionTime", avgResolutionTime != null ? avgResolutionTime : 0);
        model.addAttribute("topStaff", topStaff);
        model.addAttribute("recentComplaints", recentComplaints);
        
        return "admin/dashboard";
    }
}
