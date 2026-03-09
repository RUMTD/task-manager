package com.deployfast.taskmanager.service.impl;

import com.deployfast.taskmanager.dto.request.TaskRequest;
import com.deployfast.taskmanager.dto.response.TaskResponse;
import com.deployfast.taskmanager.exception.ResourceNotFoundException;
import com.deployfast.taskmanager.model.Task;
import com.deployfast.taskmanager.model.User;
import com.deployfast.taskmanager.model.enums.TaskStatus;
import com.deployfast.taskmanager.repository.TaskRepository;
import com.deployfast.taskmanager.repository.UserRepository;
import com.deployfast.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de gestion des tâches.
 * Applique les règles métier: un utilisateur ne peut accéder qu'à ses propres tâches.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    public TaskResponse createTask(TaskRequest request, String ownerEmail) {
        User owner = findUserByEmail(ownerEmail);

        Task task = Task.builder()
                .title(sanitize(request.getTitle()))
                .description(request.getDescription() != null ? sanitize(request.getDescription()) : null)
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .dueDate(request.getDueDate())
                .owner(owner)
                .build();

        Task saved = taskRepository.save(task);
        log.info("Tâche créée id={} par {}", saved.getId(), ownerEmail);
        return TaskResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, String ownerEmail) {
        Task task = findTaskOwnedBy(id, ownerEmail);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getMyTasks(String ownerEmail, TaskStatus status, String keyword, Pageable pageable) {
        User owner = findUserByEmail(ownerEmail);

        Page<Task> tasks;
        if (keyword != null && !keyword.isBlank()) {
            tasks = taskRepository.searchByOwnerAndKeyword(owner.getId(), keyword, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByOwnerIdAndStatus(owner.getId(), status, pageable);
        } else {
            tasks = taskRepository.findByOwnerId(owner.getId(), pageable);
        }

        return tasks.map(TaskResponse::from);
    }

    @Override
    public TaskResponse updateTask(Long id, TaskRequest request, String ownerEmail) {
        Task task = findTaskOwnedBy(id, ownerEmail);

        task.setTitle(sanitize(request.getTitle()));
        if (request.getDescription() != null) {
            task.setDescription(sanitize(request.getDescription()));
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        task.setDueDate(request.getDueDate());

        Task updated = taskRepository.save(task);
        log.info("Tâche mise à jour id={} par {}", id, ownerEmail);
        return TaskResponse.from(updated);
    }

    @Override
    public void deleteTask(Long id, String ownerEmail) {
        Task task = findTaskOwnedBy(id, ownerEmail);
        taskRepository.delete(task);
        log.info("Tâche supprimée id={} par {}", id, ownerEmail);
    }

    // === Méthodes privées utilitaires ===

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé: " + email));
    }

    private Task findTaskOwnedBy(Long taskId, String ownerEmail) {
        User owner = findUserByEmail(ownerEmail);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche", taskId));

        if (!task.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette tâche");
        }

        return task;
    }

    /**
     * Assainit les entrées pour prévenir les injections HTML/XSS.
     * Supprime les balises HTML potentiellement dangereuses.
     */
    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
