package com.example.binance_backend.controller;

import com.example.binance_backend.dto.LoginRequest;
import com.example.binance_backend.dto.LoginResponse;
import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private AuthController authController;

    @Test
    public void login_fails_when_email_not_registered() {
        // Arrange
        LoginRequest request = new LoginRequest("naoexiste@email.com", "qualquerpass");
        when(userRepo.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(404, response.getStatusCodeValue());
        assertEquals("Email não registado", ((Map<?, ?>) response.getBody()).get("email"));
    }

    @Test
    public void login_fails_when_password_is_wrong() {
        // Arrange
        User user = new User();
        user.setEmail("utilizador@email.com");
        user.setPasswordHash(BCrypt.hashpw("correta", BCrypt.gensalt()));

        LoginRequest request = new LoginRequest("utilizador@email.com", "errada");

        when(userRepo.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Password incorreta", ((Map<?, ?>) response.getBody()).get("password"));
    }

    @Test
    public void login_succeeds_with_valid_credentials() {
        // Arrange
        User user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setUsername("zequinha");
        user.setEmail("zequinha@email.com");
        user.setPasswordHash(BCrypt.hashpw("123456", BCrypt.gensalt()));

        LoginRequest request = new LoginRequest(user.getEmail(), "123456");

        when(userRepo.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());

        LoginResponse body = (LoginResponse) response.getBody();
        assertEquals(user.getUsername(), body.getUsername());
        assertEquals(user.getEmail(), body.getEmail());
        assertEquals(user.getId().toString(), body.getUserId());
    }
}
