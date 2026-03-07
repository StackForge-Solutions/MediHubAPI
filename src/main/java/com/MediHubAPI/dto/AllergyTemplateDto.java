package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllergyTemplateDto {
    private Integer id;
    private String name;
    private String category;
    private String language;
    private String contentText;
}
