package com.college.cleanliness.repository;

import com.college.cleanliness.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    Optional<User> findByResetToken(String token);
    
    Optional<User> findByEmailAndRolesName(String email, String roleName);
    
    boolean existsByEmail(String email);
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.email != 'admin@college.com'")
    int deleteAllExceptAdmin();
    
    @Query("SELECT u FROM User u WHERE u.email = 'admin@college.com'")
    Optional<User> findAdmin();
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'STAFF'")
    List<User> findAllStaff();
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'STUDENT'")
    List<User> findAllStudents();
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'STAFF' ORDER BY u.points DESC")
    List<User> findTopStaffByPoints();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'STUDENT'")
    Long countStudents();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'STAFF'")
    Long countStaff();
}
