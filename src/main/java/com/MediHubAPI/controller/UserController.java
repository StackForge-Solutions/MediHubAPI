package com.MediHubAPI.controller;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.exception.ResourceNotFoundException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Patient;
import com.MediHubAPI.service.PatientService;
import com.MediHubAPI.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// @CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final PatientService patientService;

    public UserController(UserService userService, PatientService patientService) {
        this.userService = userService;
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserCreateDto userCreateDto) {
        try {
            return new ResponseEntity<>(userService.createUser(userCreateDto), HttpStatus.CREATED);
        } catch (HospitalAPIException e) {
            logger.error("Error creating user: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating user", e);
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating user");
        }
    }


    /** Create patient with JSON + photo in the same request */
    @PostMapping(value = "/registerPatient", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PatientResponseDto> registerPatient(
            @RequestPart("data") @Valid PatientCreateDto data,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        PatientResponseDto saved = patientService.registerPatient(data, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    @GetMapping("/users-with-roles")
    public ResponseEntity<List<UserDto>> getAllUsersWithRoles() {
        List<UserDto> usersWithRoles = userService.getAllUsers();
        return ResponseEntity.ok(usersWithRoles);
    }


    @PostMapping("/register-superadmin")
    public ResponseEntity<?> registerSuperAdmin(@RequestBody UserCreateDto userCreateDto) {
        if (!userCreateDto.getRoles().contains(ERole.SUPER_ADMIN)) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Must include SUPER_ADMIN role");
        }

        return new ResponseEntity<>(userService.createUser(userCreateDto), HttpStatus.CREATED);
    }


    @PatchMapping("/username/{username}/roles")
    public ResponseEntity<UserDto> updateUserRolesByUsername(
            @PathVariable String username,
            @RequestBody Set<ERole> roles) {
        UserDto updatedUser = userService.updateUserRolesByUsername(username, roles);
        return ResponseEntity.ok(updatedUser);
    }



    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok(userService.getAllUsers());
        } catch (Exception e) {
            logger.error("Error fetching users", e);
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching users");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching user with id: {}", id, e);
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching user");
        }
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable("id") Long id,
            @RequestBody UserStatusUpdateDto statusDto) {

        userService.updateUserStatus(id, statusDto.isEnabled());
        String status = statusDto.isEnabled() ? "enabled" : "disabled";
        return ResponseEntity.ok("User account has been " + status + ".");
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting user with id: {}", id, e);
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting user");
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<List<String>> getAllRoles() {
        // ERole enum ke saare values ko get karein, unko string mein convert karein, aur ek list mein collect karein
        List<String> roles = Arrays.stream(ERole.values())
                                     .map(ERole::name)
                                     .collect(Collectors.toList());
        
        // List ko response mein OK status ke saath return karein
        return ResponseEntity.ok(roles);
    }
    @GetMapping("/patients/search")
    public ResponseEntity<Page<UserDto>> searchPatients(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.searchPatients(keyword, specialization, pageable));
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPatientPhoto(@PathVariable Long id) {
        Patient patient = patientService.findById(id);

        if (patient.getPhoto() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // or detect dynamically
                .body(patient.getPhoto());
    }


}