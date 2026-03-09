package com.deployfast.taskmanager.service.impl;

import com.deployfast.taskmanager.dto.request.LoginRequest;
import com.deployfast.taskmanager.dto.request.RegisterRequest;
import com.deployfast.taskmanager.dto.response.AuthResponse;
import com.deployfast.taskmanager.model.User;
import com.deployfast.taskmanager.model.enums.Role;
import com.deployfast.taskmanager.repository.UserRepository;
import com.deployfast.taskmanager.security.JwtService;
import com.deployfast.taskmanager.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'authentification.
 * Gère l'inscription et la connexion avec génération JWT.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email");
        }

        User user = buildUser(request);
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    private User buildUser(RegisterRequest request) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
