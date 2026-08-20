package com.college.cleanliness.config;

import com.college.cleanliness.entity.Role;
import com.college.cleanliness.entity.User;
import com.college.cleanliness.repository.RoleRepository;
import com.college.cleanliness.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(String... args) throws Exception {
        initRoles();
        initAdmin();
    }
    
    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role("ADMIN"));
            roleRepository.save(new Role("STAFF"));
            roleRepository.save(new Role("STUDENT"));
            System.out.println("Roles initialized: ADMIN, STAFF, STUDENT");
        }
    }
    
    private void initAdmin() {
        if (!userRepository.existsByEmail("admin@college.com")) {
            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            Role staffRole = roleRepository.findByName("STAFF").orElse(null);
            
            User admin = new User();
            admin.setEmail("admin@college.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setPhone("1234567890");
            admin.setDepartment("Administration");
            admin.setEnabled(true);
            admin.setRoles(Set.of(adminRole, staffRole));
            
            userRepository.save(admin);
            System.out.println("Admin user created: admin@college.com / admin123");
        }
    }
}
