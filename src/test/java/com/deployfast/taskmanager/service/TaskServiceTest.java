package com.deployfast.taskmanager.service;

import com.deployfast.taskmanager.dto.request.TaskRequest;
import com.deployfast.taskmanager.dto.response.TaskResponse;
import com.deployfast.taskmanager.exception.ResourceNotFoundException;
import com.deployfast.taskmanager.model.Task;
import com.deployfast.taskmanager.model.User;
import com.deployfast.taskmanager.model.enums.Role;
import com.deployfast.taskmanager.model.enums.TaskStatus;
import com.deployfast.taskmanager.repository.TaskRepository;
import com.deployfast.taskmanager.repository.UserRepository;
import com.deployfast.taskmanager.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - TaskService")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User owner;
    private Task task;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .build();

        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Description")
                .status(TaskStatus.TODO)
                .owner(owner)
                .build();
    }

    @Test
    @DisplayName("Créer une tâche avec succès")
    void createTask_shouldReturnTaskResponse() {
        TaskRequest request = new TaskRequest();
        request.setTitle("Nouvelle tâche");
        request.setDescription("Description");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse result = taskService.createTask(request, "user@test.com");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Task");
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Récupérer une tâche existante")
    void getTaskById_shouldReturnTask_whenOwnerMatches() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.getTaskById(1L, "user@test.com");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lever une exception si la tâche n'existe pas")
    void getTaskById_shouldThrow_whenTaskNotFound() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L, "user@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Lever AccessDeniedException si l'utilisateur n'est pas propriétaire")
    void getTaskById_shouldThrow_whenNotOwner() {
        User otherUser = User.builder().id(2L).email("other@test.com").role(Role.ROLE_USER).build();
        Task otherTask = Task.builder().id(1L).title("Other").owner(otherUser).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(otherTask));

        assertThatThrownBy(() -> taskService.getTaskById(1L, "user@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Supprimer une tâche avec succès")
    void deleteTask_shouldCallRepositoryDelete() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.deleteTask(1L, "user@test.com");

        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    @DisplayName("Mettre à jour une tâche avec succès")
    void updateTask_shouldReturnUpdatedTask() {
        TaskRequest request = new TaskRequest();
        request.setTitle("Titre modifié");
        request.setStatus(TaskStatus.IN_PROGRESS);

        Task updatedTask = Task.builder()
                .id(1L).title("Titre modifié").status(TaskStatus.IN_PROGRESS).owner(owner).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        TaskResponse result = taskService.updateTask(1L, request, "user@test.com");

        assertThat(result.getTitle()).isEqualTo("Titre modifié");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }
}
