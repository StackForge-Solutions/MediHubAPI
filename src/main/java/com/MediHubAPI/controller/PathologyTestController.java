package com.MediHubAPI.controller;


import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.PathologyTestDTO;
import com.MediHubAPI.service.PathologyTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/master-tests")
@RequiredArgsConstructor
@Slf4j
public class PathologyTestController {

    private final PathologyTestService masterTestService;

    /** 🔹 Bulk Upload (CSV) */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<PathologyTestDTO>> uploadTests(@RequestPart("file") MultipartFile file) {
        log.info("Uploading master test catalog: {}", file.getOriginalFilename());
        List<PathologyTestDTO> saved = masterTestService.bulkUploadFromCsv(file);
        return ApiResponse.success(saved, "/api/master-tests/upload", "Master tests uploaded successfully");
    }

    /** 🔹 Search by Keyword */
    @GetMapping("/search")
    public ApiResponse<List<PathologyTestDTO>> searchTests(@RequestParam String q) {
        List<PathologyTestDTO> results = masterTestService.searchTests(q);
        return ApiResponse.success(results, "/api/master-tests/search", "Search successful");
    }
}
