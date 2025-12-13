package com.MediHubAPI.service;


import com.MediHubAPI.dto.PathologyTestDTO;
import com.MediHubAPI.model.mdm.PathologyTestMaster;
import com.MediHubAPI.repository.MasterTestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PathologyTestService {

    private final MasterTestRepository masterTestRepository;
    private final ModelMapper modelMapper;

    /** ---------------- Bulk CSV Upload ---------------- */
    @Transactional
    public List<PathologyTestDTO> bulkUploadFromCsv(MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("File is empty");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<PathologyTestMaster> tests = reader.lines()
                    .skip(1)
                    .filter(line -> !line.trim().isEmpty())
                    .map(line -> line.split(",", -1))
                    .map(cols -> PathologyTestMaster.builder()
                            .name(cols[0].trim())
                            .price(parseDouble(cols[1]))
                            .tat(parseInt(cols[2]))
                            .category(cols.length > 3 ? cols[3].trim() : null)
                            .notes(cols.length > 4 ? cols[4].trim() : null)
                            .isActive(true)
                            .build())
                    .collect(Collectors.toList());

            List<PathologyTestMaster> saved = masterTestRepository.saveAllAndFlush(tests);
            return saved.stream()
                    .map(t -> modelMapper.map(t, PathologyTestDTO.class))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file", e);
        }
    }

    /** ---------------- Search by name or category ---------------- */
    public List<PathologyTestDTO> searchTests(String query) {
        return masterTestRepository.findByNameContainingIgnoreCase(query).stream()
                .map(test -> modelMapper.map(test, PathologyTestDTO.class))
                .collect(Collectors.toList());
    }

    private Double parseDouble(String val) {
        try { return val.isBlank() ? null : Double.parseDouble(val); }
        catch (Exception e) { return null; }
    }

    private Integer parseInt(String val) {
        try { return val.isBlank() ? null : Integer.parseInt(val); }
        catch (Exception e) { return null; }
    }
}

