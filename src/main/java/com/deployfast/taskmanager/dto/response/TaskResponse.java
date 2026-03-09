package com.deployfast.taskmanager.dto.response;

import com.deployfast.taskmanager.model.Task;
import com.deployfast.taskmanager.model.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour une tâche.
 * Pattern Resource: transforme l'entité en représentation API.
 */
@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String ownerEmail;

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .ownerEmail(task.getOwner().getEmail())
                .build();
    }
}
