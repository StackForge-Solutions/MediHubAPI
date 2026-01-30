package com.MediHubAPI.controller;

import com.MediHubAPI.dto.directory.DepartmentDto;
import com.MediHubAPI.dto.directory.DoctorDto;
import com.MediHubAPI.dto.directory.ListResponse;
import com.MediHubAPI.service.directory.DirectoryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping("/departments")
    public ResponseEntity<ListResponse<DepartmentDto>> departments() {
        List<DepartmentDto> data = directoryService.listDepartments();
        return ResponseEntity.ok(ListResponse.<DepartmentDto>builder()
                .status(HttpStatus.OK.value())
                .data(data)
                .message("ok")
                .build());
    }

    @GetMapping("/doctors")
    public ResponseEntity<ListResponse<DoctorDto>> doctors(
            @RequestParam(value = "departmentId", required = false) @Positive Long departmentId
    ) {
        List<DoctorDto> data = directoryService.listDoctors(departmentId);
        return ResponseEntity.ok(ListResponse.<DoctorDto>builder()
                .status(HttpStatus.OK.value())
                .data(data)
                .message("ok")
                .build());
    }
}
