package com.college.cleanliness.controller;

import com.college.cleanliness.entity.*;
import com.college.cleanliness.repository.RoleRepository;
import com.college.cleanliness.security.CustomUserDetails;
import com.college.cleanliness.service.ComplaintService;
import com.college.cleanliness.service.FileService;
import com.college.cleanliness.service.SystemResetService;
import com.college.cleanliness.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private final UserService userService;
    private final ComplaintService complaintService;
    private final RoleRepository roleRepository;
    private final FileService fileService;
    private final SystemResetService systemResetService;
    
    public AdminController(UserService userService, ComplaintService complaintService, 
                          RoleRepository roleRepository, FileService fileService,
                          SystemResetService systemResetService) {
        this.userService = userService;
        this.complaintService = complaintService;
        this.roleRepository = roleRepository;
        this.fileService = fileService;
        this.systemResetService = systemResetService;
    }
    
    @GetMapping("/users")
    public String manageUsers(
            Model model, 
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role) {
        
        List<User> users;
        if (role != null && !role.isEmpty()) {
            if (role.equals("STUDENT")) {
                users = userService.findAllStudents();
            } else if (role.equals("STAFF")) {
                users = userService.findAllStaff();
            } else {
                users = userService.findAll();
            }
        } else {
            users = userService.findAll();
        }
        
        model.addAttribute("users", users);
        model.addAttribute("user", userDetails.getUser());
        
        return "admin/users";
    }
    
    @GetMapping("/user/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/user-form";
    }
    
    @PostMapping("/user/save")
    public String saveUser(@ModelAttribute User user, @RequestParam String role) {
        Role userRole = roleRepository.findByName(role).orElse(null);
        
        Set<Role> roles = new HashSet<>();
        if (userRole != null) {
            roles.add(userRole);
        } else {
            roles.add(new Role("STUDENT"));
        }
        
        user.setRoles(roles);
        user.setEnabled(true);
        userService.saveUser(user);
        
        return "redirect:/admin/users";
    }
    
    @GetMapping("/user/{id}")
    public String viewUser(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        model.addAttribute("currentUser", userDetails.getUser());
        return "admin/user-view";
    }
    
    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/admin/users";
    }
    
    @GetMapping("/complaints")
    public String manageComplaints(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        
        Page<Complaint> complaints = complaintService.findAll(page, size, status, priority);
        
        List<User> staffList = userService.findAllStaff();
        
        model.addAttribute("complaints", complaints.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", complaints.getTotalPages());
        model.addAttribute("staffList", staffList);
        model.addAttribute("user", userDetails.getUser());
        
        return "admin/complaints";
    }
    
    @GetMapping("/complaint/{id}")
    public String viewAdminComplaint(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Complaint complaint = complaintService.findById(id);
        List<User> staffList = userService.findAllStaff();
        
        model.addAttribute("complaint", complaint);
        model.addAttribute("staffList", staffList);
        model.addAttribute("user", userDetails.getUser());
        
        return "admin/complaint-view";
    }
    
    @PostMapping("/complaint/{id}/assign")
    public String assignComplaint(
            @PathVariable Long id,
            @RequestParam Long staffId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        complaintService.assignToStaff(id, staffId, userDetails.getId());
        
        return "redirect:/admin/complaint/" + id;
    }
    
    @PostMapping("/complaint/{id}/auto-assign")
    public String autoAssignComplaint(@PathVariable Long id) {
        complaintService.autoAssign(id);
        return "redirect:/admin/complaint/" + id;
    }
    
    @PostMapping("/complaint/{id}/delete")
    public String deleteComplaint(@PathVariable Long id) {
        Complaint complaint = complaintService.findById(id);
        if (complaint != null && complaint.getImagePath() != null) {
            try {
                fileService.deleteImage(complaint.getImagePath());
            } catch (Exception e) {
                // Ignore
            }
        }
        complaintService.deleteById(id);
        return "redirect:/admin/complaints";
    }
    
    @GetMapping("/reports")
    public String reports(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long totalComplaints = complaintService.countByStatus(null);
        Long pending = complaintService.countByStatus(ComplaintStatus.PENDING);
        Long assigned = complaintService.countByStatus(ComplaintStatus.ASSIGNED);
        Long inProgress = complaintService.countByStatus(ComplaintStatus.IN_PROGRESS);
        Long cleaned = complaintService.countByStatus(ComplaintStatus.CLEANED);
        Long completed = complaintService.countByStatus(ComplaintStatus.COMPLETED);
        Long verified = complaintService.countByStatus(ComplaintStatus.VERIFIED);
        
        Double avgTime = complaintService.getAverageResolutionTime();
        
        List<User> topStaff = userService.findTopStaffByPoints();
        
        model.addAttribute("totalComplaints", totalComplaints);
        model.addAttribute("pending", pending);
        model.addAttribute("assigned", assigned);
        model.addAttribute("inProgress", inProgress);
        model.addAttribute("cleaned", cleaned);
        model.addAttribute("completed", completed);
        model.addAttribute("verified", verified);
        model.addAttribute("avgTime", avgTime != null ? avgTime : 0);
        model.addAttribute("topStaff", topStaff);
        model.addAttribute("user", userDetails.getUser());
        
        return "admin/reports";
    }
    
    @GetMapping("/reset-confirm")
    public String resetConfirm(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("totalComplaints", systemResetService.getTotalComplaints());
        model.addAttribute("totalUsers", systemResetService.getTotalUsers());
        model.addAttribute("user", userDetails.getUser());
        return "admin/reset-confirm";
    }
    
    @PostMapping("/reset-system")
    public String resetSystem(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            systemResetService.resetSystem();
            model.addAttribute("message", "System has been reset successfully. All data has been cleared except the admin account.");
        } catch (Exception e) {
            model.addAttribute("error", "Error resetting system: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?reset=success";
    }
}
