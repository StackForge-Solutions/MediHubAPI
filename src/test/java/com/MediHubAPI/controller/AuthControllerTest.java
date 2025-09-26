package com.MediHubAPI.controller;


import com.MediHubAPI.dto.JwtAuthResponse;
import com.MediHubAPI.dto.LoginDto;
import com.MediHubAPI.dto.RegisterDto;
import com.MediHubAPI.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLogin_ReturnsJwtAuthResponse() {
        // Arrange
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("user@example.com");
        loginDto.setPassword("password");

        JwtAuthResponse jwtResponse = new JwtAuthResponse();
        jwtResponse.setAccessToken("mocked-jwt-token");
        jwtResponse.setTokenType("Bearer");

        when(authService.login(loginDto)).thenReturn(jwtResponse);

        // Act
        ResponseEntity<JwtAuthResponse> response = authController.login(loginDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("mocked-jwt-token", response.getBody().getAccessToken());
        verify(authService, times(1)).login(loginDto);
    }

    @Test
    void testRegister_ReturnsSuccessMessage() {
        // Arrange
        RegisterDto registerDto = new RegisterDto();
       // registerDto.setName("Amit");
        registerDto.setUsername("amit123");
        registerDto.setEmail("amit@example.com");
        registerDto.setPassword("password");

        String successMessage = "User registered successfully!";
        when(authService.register(registerDto)).thenReturn(successMessage);

        // Act
        ResponseEntity<String> response = authController.register(registerDto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(successMessage, response.getBody());
        verify(authService, times(1)).register(registerDto);
    }
}
