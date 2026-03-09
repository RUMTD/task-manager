package com.deployfast.taskmanager.service;

import com.deployfast.taskmanager.dto.request.TaskRequest;
import com.deployfast.taskmanager.dto.response.TaskResponse;
import com.deployfast.taskmanager.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrat du service de gestion des tâches.
 */
public interface TaskService {

    TaskResponse createTask(TaskRequest request, String ownerEmail);

    TaskResponse getTaskById(Long id, String ownerEmail);

    Page<TaskResponse> getMyTasks(String ownerEmail, TaskStatus status, String keyword, Pageable pageable);

    TaskResponse updateTask(Long id, TaskRequest request, String ownerEmail);

    void deleteTask(Long id, String ownerEmail);
}
