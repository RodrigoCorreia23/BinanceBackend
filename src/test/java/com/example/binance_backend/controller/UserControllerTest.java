package com.example.binance_backend.controller;

import com.example.binance_backend.dto.SignUpRequest;
import com.example.binance_backend.dto.SignUpResponse;
import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private UserController userController;

    @Test
    public void signup_fails_when_email_already_exists() {
        // Arrange
        SignUpRequest request = new SignUpRequest();
        request.setEmail("teste@email.com");
        request.setUsername("utilizador");
        request.setPassword("123456");

        when(userRepo.existsByEmail(request.getEmail())).thenReturn(true);
        when(userRepo.existsByUsername(request.getUsername())).thenReturn(false);

        // Act
        ResponseEntity<?> response = userController.signup(request);

        // Assert
        assertEquals(409, response.getStatusCodeValue()); // CONFLICT
        assertEquals("Email já existe", ((Map<?, ?>) response.getBody()).get("email"));
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    public void signup_fails_when_username_already_exists() {
        // Arrange
        SignUpRequest request = new SignUpRequest();
        request.setEmail("novo@email.com");
        request.setUsername("utilizadorExistente");
        request.setPassword("123456");

        when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepo.existsByUsername(request.getUsername())).thenReturn(true);

        // Act
        ResponseEntity<?> response = userController.signup(request);

        // Assert
        assertEquals(409, response.getStatusCodeValue()); // CONFLICT
        assertEquals("Nome de utilizador já existe.", ((Map<?, ?>) response.getBody()).get("username"));
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    public void signup_fails_when_both_email_and_username_exist() {
        // Arrange
        SignUpRequest request = new SignUpRequest();
        request.setEmail("existe@email.com");
        request.setUsername("utilizadorExistente");
        request.setPassword("123456");

        when(userRepo.existsByEmail(request.getEmail())).thenReturn(true);
        when(userRepo.existsByUsername(request.getUsername())).thenReturn(true);

        // Act
        ResponseEntity<?> response = userController.signup(request);

        // Assert
        assertEquals(409, response.getStatusCodeValue()); // CONFLICT
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Email já existe", body.get("email"));
        assertEquals("Nome de utilizador já existe.", body.get("username"));
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    public void signup_succeeds_with_valid_data() {
        // Arrange
        SignUpRequest request = new SignUpRequest();
        request.setEmail("novo@email.com");
        request.setUsername("novoUtilizador");
        request.setPassword("123456");

        User savedUser = new User();
        savedUser.setId(java.util.UUID.randomUUID());
        savedUser.setEmail(request.getEmail());
        savedUser.setUsername(request.getUsername());
        savedUser.setPasswordHash("$2a$10$hashedPassword"); // Simular password hash

        when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepo.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(savedUser);

        // Act
        ResponseEntity<?> response = userController.signup(request);

        // Assert
        assertEquals(201, response.getStatusCodeValue()); // CREATED

        SignUpResponse body = (SignUpResponse) response.getBody();
        assertNotNull(body);
        assertEquals(savedUser.getId().toString(), body.getUserId());
        assertEquals(savedUser.getUsername(), body.getUsername());

        // Verificar que o save foi chamado
        verify(userRepo, times(1)).save(any(User.class));
    }
}