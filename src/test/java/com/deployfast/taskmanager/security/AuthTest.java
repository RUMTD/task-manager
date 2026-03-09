package com.deployfast.taskmanager.security;

import com.deployfast.taskmanager.dto.request.LoginRequest;
import com.deployfast.taskmanager.dto.request.RegisterRequest;
import com.deployfast.taskmanager.dto.response.AuthResponse;
import com.deployfast.taskmanager.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.deployfast.taskmanager.config.SecurityConfig;
import com.deployfast.taskmanager.controller.AuthController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, UserDetailsServiceImpl.class})
@DisplayName("Tests d'authentification")
class AuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.deployfast.taskmanager.repository.UserRepository userRepository;

    @Test
    @DisplayName("POST /auth/register - Inscription réussie")
    void register_withValidData_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@test.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .type("Bearer")
                .email("john@test.com")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /auth/register - Email invalide retourne 400")
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("invalid-email");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - Connexion réussie")
    void login_withValidCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@test.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .type("Bearer")
                .email("john@test.com")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    @DisplayName("POST /auth/login - Champs vides retournent 400")
    void login_withMissingFields_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        // Champs manquants intentionnellement

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
