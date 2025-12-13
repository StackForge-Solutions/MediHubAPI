package com.MediHubAPI.dto;
// For payload like: { "General": { "Systemic": ["Fever", ...], ... }, ... }
import java.util.List;
import java.util.Map;

public record SymptomCatalogDto(Map<String, Map<String, List<String>>> categories) {}
