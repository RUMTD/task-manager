package com.deployfast.taskmanager.controller;

import com.deployfast.taskmanager.dto.request.TaskRequest;
import com.deployfast.taskmanager.dto.response.TaskResponse;
import com.deployfast.taskmanager.model.enums.TaskStatus;
import com.deployfast.taskmanager.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.deployfast.taskmanager.config.SecurityConfig;
import com.deployfast.taskmanager.security.JwtAuthenticationFilter;
import com.deployfast.taskmanager.security.JwtService;
import com.deployfast.taskmanager.security.UserDetailsServiceImpl;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, UserDetailsServiceImpl.class})
@DisplayName("Tests Feature - TaskController")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private com.deployfast.taskmanager.repository.UserRepository userRepository;

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /tasks - Créer une tâche avec données valides")
    void createTask_withValidData_returns201() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Ma nouvelle tâche");

        TaskResponse response = TaskResponse.builder()
                .id(1L).title("Ma nouvelle tâche").status(TaskStatus.TODO).build();

        when(taskService.createTask(any(TaskRequest.class), eq("user@test.com")))
                .thenReturn(response);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Ma nouvelle tâche"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /tasks - Rejeter une tâche sans titre")
    void createTask_withBlankTitle_returns400() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("");

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /tasks - Requête sans token retourne 401")
    void getTasks_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /tasks - Lister les tâches avec pagination")
    void getTasks_withAuth_returns200() throws Exception {
        Page<TaskResponse> page = new PageImpl<>(List.of());
        when(taskService.getMyTasks(eq("user@test.com"), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/tasks")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /tasks/{id} - Supprimer une tâche existante")
    void deleteTask_withValidId_returns200() throws Exception {
        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isOk());
    }
}
