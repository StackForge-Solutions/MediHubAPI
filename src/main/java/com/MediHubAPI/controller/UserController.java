package com.MediHubAPI.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.MediHubAPI.dto.PaginationRequestDto;
import com.MediHubAPI.dto.UserCreateDto;
import com.MediHubAPI.dto.UserDto;
import com.MediHubAPI.dto.UserUpdateDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.exception.ResourceNotFoundException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.User;
import com.MediHubAPI.service.PatientService;
import com.MediHubAPI.service.UserService;

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
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateDto userCreateDto) {
        return new ResponseEntity<>(userService.createUser(userCreateDto), HttpStatus.CREATED);
    }

    @PostMapping("/register-superadmin")
    public ResponseEntity<?> registerSuperAdmin(@Valid @RequestBody UserCreateDto userCreateDto) {
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
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @Valid @ModelAttribute PaginationRequestDto pagination) {
        try {
            Pageable pageable = PageRequest.of(pagination.getPage(), pagination.getSize());
            return ResponseEntity.ok(userService.getAllUsers(pageable));
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

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto userUpdateDto) {
        return ResponseEntity.ok(userService.updateUser(id, userUpdateDto));
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
        User patient = patientService.findById(id);
        byte[] photo = patient.getPhoto();
        if (photo == null || photo.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // Try stored content type first; fall back to octet-stream if invalid/absent
        MediaType mediaType = resolveMediaType(patient.getPhotoContentType());

        // Nice-to-have: inline filename with proper extension
        String ext = mediaType.getSubtype(); // e.g., "png", "jpeg", "webp"
        String fileName = "patient-" + id + "." + ext;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentLength(photo.length)
                .body(photo);
    }

    private MediaType resolveMediaType(String stored) {
        try {
            if (stored != null && !stored.isBlank()) {
                return MediaType.parseMediaType(stored); // e.g., "image/png"
            }
        } catch (org.springframework.http.InvalidMediaTypeException ignore) {
            // fall through to default
        }
        return MediaType.APPLICATION_OCTET_STREAM; // safe fallback when unknown
    }


}
