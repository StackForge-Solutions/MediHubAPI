package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.JwtAuthResponse;
import com.MediHubAPI.dto.LoginDto;
import com.MediHubAPI.dto.RegisterDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Role;
import com.MediHubAPI.model.User;
import com.MediHubAPI.repository.RoleRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.security.JwtTokenProvider;
import com.MediHubAPI.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public JwtAuthResponse login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByUsernameOrEmailWithRoles(authentication.getName())
                .orElseThrow(() -> new HospitalAPIException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Authenticated user record not found"
                ));
        String token = tokenProvider.generateToken(user);

        return new JwtAuthResponse(token);
    }

    @Override
    public String register(RegisterDto registerDto) {
        // Check if username exists
        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        // Check if email exists
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        System.out.println(passwordEncoder.encode("admin123"));


        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setFirstName(registerDto.getFirstName());
        user.setLastName(registerDto.getLastName());

        // Assign PATIENT role
        Role role = roleRepository.findByName(ERole.PATIENT)
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not found"));

        user.setRoles(Collections.singleton(role));
        userRepository.save(user);

        return "User registered successfully";
    }
}
