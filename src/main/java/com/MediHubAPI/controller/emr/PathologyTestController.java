package com.MediHubAPI.controller.emr;


import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.PathologyTestDTO;
import com.MediHubAPI.service.PathologyTestService;

@RestController
@RequestMapping("/api/master-tests")
@RequiredArgsConstructor
@Slf4j
public class PathologyTestController {

    private final PathologyTestService masterTestService;

    /**
     * 🔹 Bulk Upload (CSV)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<PathologyTestDTO>> uploadTests(@RequestPart("file") MultipartFile file) {
        log.info("Uploading master test catalog: {}", file.getOriginalFilename());
        List<PathologyTestDTO> saved = masterTestService.bulkUploadFromCsv(file);
        return ApiResponse.success(saved, "/api/master-tests/upload", "Master tests uploaded successfully");
    }

    /**
     * 🔹 Search by Keyword
     */
    @GetMapping("/search")
    public ApiResponse<List<PathologyTestDTO>> searchTests(@RequestParam String q) {
        List<PathologyTestDTO> results = masterTestService.searchTests(q);
        return ApiResponse.success(results, "/api/master-tests/search", "Search successful");
    }
}
