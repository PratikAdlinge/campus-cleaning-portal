package com.college.cleanliness.service;

import com.college.cleanliness.entity.Role;
import com.college.cleanliness.entity.User;
import com.college.cleanliness.repository.RoleRepository;
import com.college.cleanliness.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public User findByResetToken(String token) {
        return userRepository.findByResetToken(token).orElse(null);
    }
    
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Transactional
    public User saveUser(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    public List<User> findAllStaff() {
        return userRepository.findAllStaff();
    }
    
    public List<User> findAllStudents() {
        return userRepository.findAllStudents();
    }
    
    public List<User> findTopStaffByPoints() {
        return userRepository.findTopStaffByPoints();
    }
    
    public Long countStudents() {
        return userRepository.countStudents();
    }
    
    public Long countStaff() {
        return userRepository.countStaff();
    }
    
    @Transactional
    public void addRoleToUser(User user, String roleName) {
        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role != null) {
            user.getRoles().add(role);
            userRepository.save(user);
        }
    }
    
    @Transactional
    public void removeRoleFromUser(User user, String roleName) {
        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role != null) {
            user.getRoles().remove(role);
            userRepository.save(user);
        }
    }
    
    @Transactional
    public void updatePoints(User user, int points) {
        user.setPoints(user.getPoints() + points);
        userRepository.save(user);
    }
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
