package com.MediHubAPI.specification;

import com.MediHubAPI.dto.patient.search.PatientSearchBy;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.User;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class PatientSearchSpecification {

    public static Specification<User> matches(PatientSearchBy by, String query) {
        return switch (by) {
            case NAME -> searchName(query);
            case PHONE -> searchExact("mobileNumber", query);
            case HOSPITAL_ID -> searchIgnoreCase("hospitalId", query);
            case FILE_NO -> searchIgnoreCase("fileNo", query);
            case FATHER_NAME -> searchIgnoreCase("fathersName", query);
            case MOTHER_NAME -> searchIgnoreCase("mothersName", query);
            case DOB -> searchDob(query);
        };
    }

    private static Specification<User> searchName(String query) {
        String like = likePattern(query);
        return (root, criteriaQuery, cb) -> {
            Expression<String> first = root.get("firstName");
            Expression<String> last = root.get("lastName");
            Expression<String> fullNameLower = cb.concat(
                    cb.concat(cb.lower(first), " "), cb.lower(last)
            );
            return cb.or(
                    cb.like(cb.lower(first), like),
                    cb.like(cb.lower(last), like),
                    cb.like(fullNameLower, like)
            );
        };
    }

    private static Specification<User> searchExact(String field, String q) {
        return (root, query, cb) -> cb.equal(root.get(field), q);
    }

    private static Specification<User> searchIgnoreCase(String field, String q) {
        String lowered = q.toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get(field)), lowered);
    }

    private static Specification<User> searchDob(String q) {
        LocalDate date;
        try {
            date = LocalDate.parse(q);
        } catch (DateTimeParseException ex) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "q must be a valid ISO date when by=DOB");
        }
        return (root, query, cb) -> cb.equal(root.get("dateOfBirth"), date);
    }

    private static String likePattern(String text) {
        return "%" + text.toLowerCase() + "%";
    }
}
