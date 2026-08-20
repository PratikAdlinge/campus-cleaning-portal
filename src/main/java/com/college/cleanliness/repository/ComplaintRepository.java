package com.college.cleanliness.repository;

import com.college.cleanliness.entity.Complaint;
import com.college.cleanliness.entity.ComplaintStatus;
import com.college.cleanliness.entity.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    
    void deleteAll();
    
    Page<Complaint> findByUserId(Long userId, Pageable pageable);
    
    Page<Complaint> findByStaffId(Long staffId, Pageable pageable);
    
    List<Complaint> findByStatus(ComplaintStatus status);
    
    @Query("SELECT c FROM Complaint c WHERE c.status = :status AND c.staff.id = :staffId")
    List<Complaint> findByStatusAndStaffId(@Param("status") ComplaintStatus status, @Param("staffId") Long staffId);
    
    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('COMPLETED', 'VERIFIED')")
    List<Complaint> findActiveComplaints();
    
    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.status = :status")
    Long countByStatus(@Param("status") ComplaintStatus status);
    
    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.staff.id = :staffId AND c.status = 'COMPLETED'")
    Long countCompletedByStaffId(@Param("staffId") Long staffId);
    
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, c.createdAt, c.completedAt)) FROM Complaint c WHERE c.status = 'VERIFIED' AND c.completedAt IS NOT NULL")
    Double getAverageResolutionTimeMinutes();
    
    @Query("SELECT c.locationType, COUNT(c) FROM Complaint c GROUP BY c.locationType")
    List<Object[]> countByLocationType();
    
    @Query("SELECT c FROM Complaint c WHERE c.createdAt BETWEEN :start AND :end")
    List<Complaint> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT c FROM Complaint c WHERE c.priority = :priority AND c.status NOT IN ('COMPLETED', 'VERIFIED')")
    List<Complaint> findByPriorityAndActive(@Param("priority") Priority priority);
    
    @Query("SELECT c FROM Complaint c WHERE c.status = 'PENDING' ORDER BY c.priority DESC, c.createdAt ASC")
    List<Complaint> findPendingComplaintsSorted();
}
