package com.deployfast.taskmanager.controller;

import com.deployfast.taskmanager.dto.request.LoginRequest;
import com.deployfast.taskmanager.dto.request.RegisterRequest;
import com.deployfast.taskmanager.dto.response.ApiResponse;
import com.deployfast.taskmanager.dto.response.AuthResponse;
import com.deployfast.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'authentification.
 * Routes publiques: /api/v1/auth/**
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Créer un nouveau compte")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Compte créé avec succès", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter et obtenir un token JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Connexion réussie", response));
    }
}
