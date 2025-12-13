package com.MediHubAPI.repository.specs;

import com.MediHubAPI.model.billing.DoctorServiceItem;
import com.MediHubAPI.model.enums.ServiceStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class DoctorServiceItemSpecs {

    public static Specification<DoctorServiceItem> forDoctor(Long doctorId) {
        return (root, q, cb) -> cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<DoctorServiceItem> nameLike(String qStr) {
        if (qStr == null || qStr.isBlank()) return null;
        String like = "%" + qStr.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), like);
    }

    public static Specification<DoctorServiceItem> withStatus(ServiceStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<DoctorServiceItem> minPrice(BigDecimal min) {
        if (min == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<DoctorServiceItem> maxPrice(BigDecimal max) {
        if (max == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }
}
