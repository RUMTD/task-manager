package com.deployfast.taskmanager.service;

import com.deployfast.taskmanager.dto.request.LoginRequest;
import com.deployfast.taskmanager.dto.request.RegisterRequest;
import com.deployfast.taskmanager.dto.response.AuthResponse;

/**
 * Contrat du service d'authentification.
 * Principe ISP: interface focalisée sur l'authentification uniquement.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
