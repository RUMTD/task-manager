package com.deployfast.taskmanager.repository;

import com.deployfast.taskmanager.model.Task;
import com.deployfast.taskmanager.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository pour les opérations de persistance des tâches.
 * Supporte la pagination et le filtrage par propriétaire.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.owner.id = :ownerId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Task> searchByOwnerAndKeyword(@Param("ownerId") Long ownerId,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    boolean existsByIdAndOwnerId(Long taskId, Long ownerId);
}
