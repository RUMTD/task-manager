package com.deployfast.taskmanager.dto.request;

import com.deployfast.taskmanager.model.enums.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO pour la création et mise à jour d'une tâche.
 */
@Data
public class TaskRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne peut dépasser 2000 caractères")
    private String description;

    private TaskStatus status;

    @FutureOrPresent(message = "La date d'échéance doit être présente ou future")
    private LocalDateTime dueDate;
}
