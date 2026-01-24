package com.MediHubAPI.repository.scheduling.template;

import org.springframework.data.jpa.domain.Specification;
import com.MediHubAPI.model.enums.TemplateScope;
import com.MediHubAPI.model.scheduling.template.ScheduleTemplate;

public final class TemplateSpecifications {

    private TemplateSpecifications() {}

    public static Specification<ScheduleTemplate> scopeEq(TemplateScope scope) {
        return (root, q, cb) -> scope == null ? cb.conjunction() : cb.equal(root.get("scope"), scope);
    }

    public static Specification<ScheduleTemplate> doctorIdEq(Long doctorId) {
        return (root, q, cb) -> doctorId == null ? cb.conjunction() : cb.equal(root.get("doctorId"), doctorId);
    }

    public static Specification<ScheduleTemplate> departmentIdEq(Long departmentId) {
        return (root, q, cb) -> departmentId == null ? cb.conjunction() : cb.equal(root.get("departmentId"),
                departmentId);
    }

    public static Specification<ScheduleTemplate> activeEq(Boolean active) {
        return (root, q, cb) -> active == null ? cb.conjunction() : cb.equal(root.get("active"), active);
    }

    public static Specification<ScheduleTemplate> nameLike(String qText) {
        return (root, q, cb) -> {
            if (qText == null || qText.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + qText.trim().toLowerCase() + "%");
        };
    }
}
