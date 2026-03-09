package com.deployfast.taskmanager.controller;

import com.deployfast.taskmanager.dto.request.TaskRequest;
import com.deployfast.taskmanager.dto.response.ApiResponse;
import com.deployfast.taskmanager.dto.response.TaskResponse;
import com.deployfast.taskmanager.model.enums.TaskStatus;
import com.deployfast.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST CRUD pour les tâches.
 * Toutes les routes nécessitent une authentification JWT.
 * Un utilisateur ne peut accéder qu'à ses propres tâches.
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tâches", description = "CRUD des tâches")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Créer une nouvelle tâche")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetails user) {

        TaskResponse task = taskService.createTask(request, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tâche créée", task));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une tâche par ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {

        TaskResponse task = taskService.getTaskById(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(task));
    }

    @GetMapping
    @Operation(summary = "Lister mes tâches avec pagination et filtres")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getMyTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserDetails user) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TaskResponse> tasks = taskService.getMyTasks(user.getUsername(), status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(tasks));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une tâche")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetails user) {

        TaskResponse updated = taskService.updateTask(id, request, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Tâche mise à jour", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une tâche")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {

        taskService.deleteTask(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Tâche supprimée", null));
    }
}
